package io.kani.applock

import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.Future


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
    private val globalLock = Lock(lockDir.resolve("global.lock"))
    private val lock = Lock(lockDir.resolve(id.encoded() + ".lock"))
    private val portFile = lockDir.resolve(id.encoded() + ".portFile")
    private val shutdownHook = Thread({ unlockInternal() }, "AppLocker `$id` unlock shutdownHook")

    private var msgServerHandle: Future<*>? = null
    private val msgClient = MessageClient(id)

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
        msgServerHandle = executor.submit(messageServer)
        // retrieve server port number and store it to file
        Files.write(portFile, messageServer.portNumber.toString().toByteArray())

        // add shutdown shutdownHook
        val rt = Runtime.getRuntime()
        rt.addShutdownHook(shutdownHook)

        return LockResult.Success
    }

    /**
     * Unlocks the resources.
     * Does not throw.
     */
    fun unlock(): Unit = globalLock.use { gLock ->
        gLock.loopLock()

        val rt = Runtime.getRuntime()
        rt.removeShutdownHook(shutdownHook)

        // terminate message server
        msgServerHandle?.cancel(true)

        // unlock the file
        lock.unlock()
        portFile.toFile().delete()
    }

    /**
     * Exact copy of [unlock], except there is no [Runtime.removeShutdownHook]
     */
    private fun unlockInternal(): Unit = globalLock.use { gLock ->
        gLock.loopLock()

        // terminate message server
        msgServerHandle?.cancel(true)

        // unlock the file
        lock.unlock()
        portFile.toFile().delete()
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
        if (!Files.exists(portFile)) {
            return Response.Failure("Port file for $id not found, cannot send message")
        }
        val portNumber = String(Files.readAllBytes(portFile)).toInt()

        msgClient.sendMessage(msg, portNumber)
    }
}

/**
 * Extension wrapper for [URLEncoder.encode] with [Charsets.UTF_8] charset
 */
private fun String.encoded(): String = URLEncoder.encode(this, Charsets.UTF_8)


