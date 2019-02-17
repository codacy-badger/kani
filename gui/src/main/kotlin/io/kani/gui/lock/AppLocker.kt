package io.kani.gui.lock

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.channels.*
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.concurrent.thread


typealias MessageHandler = (message: String) -> String


/**
 * Result of locking call
 */
sealed class LockResult {
    object Success : LockResult()
    data class LockFailed(val description: String) : LockResult()
    data class UnableCreateLock(val description: String) : LockResult()
}

/**
 * Result of message
 */
sealed class Response {
    data class Answer(val answer: String) : Response()
    data class Failure(val description: String) : Response()
}


/**
 * Lock file manager, provides methods for locking mechanism and
 * encapsulates simple socket-based message server for IPC.
 *
 * You don't need call [unlock] directly, method will be called once JVM is terminated.
 * You're free to call [unlock] any time if it required by your application logic.
 *
 *
 * @param id Lock identifier, accepts any string.
 *           This field will be processed by [URLEncoder.encode], but there are still other limitations by OS
 *           like forbidden names (i.e. COM, NUL etc), as well as filename length
 * @param lockDir Directory where lock files will be stored. Must be the same
 *                for all application instances which want to share the same lock id
 * @param messageHandler Handler for messages from other application instances
 */
class AppLocker(val id: String, lockDir: Path, private val messageHandler: MessageHandler = { it }) {
    /** Used to synchronize [AppLocker] between processes */
    private val globalLock = Lock(lockDir.resolve("global.lock"))
    /** File-based lock */
    private val lock = Lock(lockDir.resolve(id.encoded() + ".lock"))
    /** This extra file is used to pass server port between instances */
    private val port = lockDir.resolve(id.encoded() + ".port")
    /** Runtime hook, which automatically added with [Runtime.addShutdownHook] on [lock] and removed on [unlock] */
    private val hook = thread(name = "AppLocker[$id]", start = false) { unlockInternal() }

    private val messageExecutor = Executors.newSingleThreadExecutor { task ->
        Thread(task).apply {
            name = "AppLocker `${this@AppLocker.id}` server thread"
            isDaemon = true
        }
    }
    /** Handler for socket-based server used to accept messages from [AppLocker.sendMessage] with the same [id] */
    private var server: Future<*>? = null

    /**
     * Indicates if this lock is currently in use (locked)
     */
    val isLocked: Boolean
        get() = lock.isLocked

    /**
     * Attempts to lock the [id] lock
     * @return lock result
     */
    fun lock(): LockResult = globalLock.use { gLock ->
        val lockResult = gLock.loopLock()
        if (lockResult is LockResult.UnableCreateLock) {
            return lockResult
        }

        when (val result = lock.lock()) {
            is LockResult.LockFailed -> return result
        }

        // create server: open selector, channel
        val messageServer = MessageServer(messageHandler)
        // query the server to execution
        val executor = Executors.newSingleThreadExecutor { task ->
            Thread(task).apply {
                name = "AppLocker `${this@AppLocker.id}` server thread"
                isDaemon = true
            }
        }
        server = executor.submit(messageServer)
        // retrieve the port number (binding happens in server ctor, this is not dynamic information)
        Files.write(port, messageServer.port.toString().toByteArray())

        // add shutdown hook
        val rt = Runtime.getRuntime()
        rt.addShutdownHook(hook)

        return LockResult.Success
    }

    /**
     * Unlocks the resources.
     * Does not throw.
     */
    fun unlock(): Unit = globalLock.use { gLock ->
        gLock.loopLock()

        val rt = Runtime.getRuntime()
        rt.removeShutdownHook(hook)

        // terminate message server
        server?.cancel(true)

        // unlock the file
        lock.unlock()

        // delete files
        try {
            lock.file.toFile().delete()
            port.toFile().delete()
        } catch (_: Throwable) {
        }
    }

    /**
     * Exact copy of [unlock], except there is no [Runtime.removeShutdownHook]
     */
    private fun unlockInternal(): Unit = globalLock.use { gLock ->
        gLock.loopLock()

        // terminate message server
        server?.cancel(true)

        // unlock the file
        lock.unlock()

        // delete files
        try {
            lock.file.toFile().delete()
            port.toFile().delete()
        } catch (_: Throwable) {
        }
    }

    /**
     * Send a message to the running instance
     *
     * @param msg the message to send, size is limited by 1024 bytes
     * @return result of the operation
     */
    fun sendMessage(msg: String): Response = globalLock.use { gLock ->
        val lockResult = gLock.loopLock()
        if (lockResult is LockResult.UnableCreateLock) {
            return Response.Failure(lockResult.description)
        }

        val response = messageExecutor.submit(Callable {
            if (!Files.exists(port)) {
                return@Callable Response.Failure("Port file for $id not found, can't send message")
            }

            // read the server port number
            val portNum = String(Files.readAllBytes(port)).toInt()

            // open the socket, send message and retrieve response
            SocketChannel.open(InetSocketAddress(InetAddress.getLocalHost(), portNum)).use { server ->

                // send message to server
                server.write(ByteBuffer.wrap(msg.toByteArray()))

                // wait for response
                val buffer = ByteBuffer.allocate(1024)
                val message = readSocket(server, buffer)
                Response.Answer(message)
            }
        })
        return@use response.get()
    }
}

/**
 * Internal wrapper around file object for automatic management of [FileChannel] and [FileLock] objects
 */
private class Lock(val file: Path) : AutoCloseable {
    private var lockChannel: FileChannel? = null
    private var lock: FileLock? = null

    val isLocked: Boolean
        get() = lockChannel != null

    /**
     * Attempts to lock the file in do-while loop.
     * Does not throw.
     */
    fun loopLock(): LockResult {
        loop@ while (true) return when (val lock = lock()) {
            LockResult.Success -> lock
            is LockResult.UnableCreateLock -> lock
            is LockResult.LockFailed -> continue@loop
        }
    }

    /**
     * Attempts to lock the file
     * @return returns [LockResult.Success] if successful, otherwise return [LockResult.LockFailed]
     */
    fun lock(): LockResult {
        try {
            if (!Files.exists(file.parent, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    Files.createDirectories(file.parent)
                } catch (ex: Exception) {
                    return LockResult.UnableCreateLock(ex.message ?: "Failed to create ${file.parent} dir")
                }
            }
            if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
                try {
                    Files.createFile(file)
                } catch (ex: Exception) {
                    return LockResult.UnableCreateLock(ex.message ?: "Failed to create $file file")
                }

            }

            val channel = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE)

            val tryLock: FileLock? =
                channel.tryLock() ?: return LockResult.LockFailed("$file is already locked")

            lockChannel = channel
            lock = tryLock

            return LockResult.Success
        } catch (t: Throwable) {
            return LockResult.LockFailed(t.message ?: "Unable to lock $file")
        }
    }

    /**
     * Unlocks the resources.
     * Does not throw.
     */
    fun unlock(): Unit = try {
        lockChannel?.close()
        lockChannel = null

        lock?.release()
        lock = null
    } catch (t: Throwable) {
    }

    override fun close() {
        unlock()
    }
}


/**
 * Internal server for communication between [AppLocker] instances.
 *
 * Server reads client message once and discards connection
 *
 * Make sure to execute [run] (via thread or executors), otherwise selector and channel won't be closed
 */
private class MessageServer(private val messageHandler: MessageHandler) : Runnable {
    private val selector: Selector = Selector.open()
    private val channel: ServerSocketChannel
    val port: Int

    init {
        channel = ServerSocketChannel.open().apply {
            bind(InetSocketAddress(InetAddress.getLocalHost(), 0))
            configureBlocking(false)
            register(selector, SelectionKey.OP_ACCEPT)
        }
        port = channel.socket().localPort
    }

    override fun run() {
        selector.use { selector ->
            channel.use { socket ->
                val buffer = ByteBuffer.allocate(1024)

                while (selector.isOpen && channel.isOpen) {
                    if (Thread.currentThread().isInterrupted) return

                    selector.select()
                    val selectedKeys = selector.selectedKeys()
                    val iter = selectedKeys.iterator()

                    while (iter.hasNext()) {
                        if (Thread.currentThread().isInterrupted) return

                        val key = iter.next()

                        // establish connection
                        if (key.isAcceptable) {
                            val client = socket.accept()
                            client.configureBlocking(false)
                            client.register(selector, SelectionKey.OP_READ)
                        }

                        // process client message
                        if (key.isReadable) {
                            (key.channel() as SocketChannel).use { client ->
                                buffer.clear()

                                val message = readSocket(client, buffer)
                                val answer = messageHandler(message)
                                client.write(ByteBuffer.wrap(answer.toByteArray()))
                            }
                        }
                        iter.remove()
                    }
                }
            }
        }
    }


}


/**
 * Extension wrapper for [URLEncoder.encode] with [Charsets.UTF_8] charset
 */
private fun String.encoded(): String = URLEncoder.encode(this, Charsets.UTF_8)

/**
 * Reads data from socket channel to string
 */
private fun readSocket(client: SocketChannel, buffer: ByteBuffer): String {
    val sb = StringBuilder()
    while (true) {
        if (client.read(buffer) <= 0) break

        buffer.flip()
        val bytes = ByteArray(buffer.limit())
        buffer.get(bytes)
        sb.append(String(bytes))
        buffer.clear()
    }
    return sb.toString()
}
