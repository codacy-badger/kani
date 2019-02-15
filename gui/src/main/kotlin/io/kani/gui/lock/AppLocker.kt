package io.kani.gui.lock

import java.io.*
import java.net.InetAddress
import java.net.ServerSocket
import java.net.URLEncoder
import java.nio.file.Path
import kotlin.concurrent.thread

/**
 * Extension wrapper for [URLEncoder.encode] with [Charsets.UTF_8] charset
 */
private fun String.encoded(): String = URLEncoder.encode(this, Charsets.UTF_8)

typealias MessageHandler = (String) -> Unit

sealed class LockResult {
    object Success : LockResult()
    data class AlreadyLocked(val description: String) : LockResult()
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
 * @param messageHandler Optional handler for messages from other application instances
 */
class AppLocker(private val id: String, lockDir: Path, val messageHandler: MessageHandler = {}) {
    /** Used to synchronize [AppLocker] between processes */
    private val globalLock = Lock(lockDir.resolve("global.lock"))
    /** File-based lock */
    private val lock = Lock(lockDir.resolve(id.encoded() + ".lock"))
    /** This extra file is used to pass server port between instances */
    private val port = lockDir.resolve(id.encoded() + ".port")

    /** Socket-based server used to accept messages from [AppLocker.sendMessage] with the same [id] */
    private var server: SimpleMessageServer? = null

    init {
        // runtime hook to unlock resources
        val rt = Runtime.getRuntime()
        rt.addShutdownHook(thread(name = id, start = false) { unlock() })
    }

    /**
     * Attempts to lock the [id] lock
     * @return lock result
     */
    fun lock(): LockResult = globalLock.use { gLock ->
        gLock.loopLock()

        when (val result = lock.lock()) {
            is LockResult.AlreadyLocked -> return result
        }
        server = null // TODO obtain server instance
        return LockResult.Success
    }

    /**
     * Unlocks the resources.
     * Does not throw.
     */
    fun unlock(): Unit = globalLock.use { gLock ->
        gLock.loopLock()

        // terminate message server
        server?.stop()

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
     * @return result of the operation
     */
    fun sendMessage(msg: String): Unit = println()//server.sendMessage(msg)
}
