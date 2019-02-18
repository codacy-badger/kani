package io.kani.applock

import java.io.IOException
import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.channels.OverlappingFileLockException
import java.nio.file.Files
import java.nio.file.LinkOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption


/**
 * Result of locking call
 */
sealed class LockResult {
    /** Indicates about successful locking */
    object Success : LockResult()

    /** Indicates that lock was already acquired */
    object LockFailed : LockResult()

    /** Indicates about failed locking, unable create file for locking */
    data class UnableCreateLock(val description: String) : LockResult()
}

/**
 * Internal wrapper around file object for automatic management of [FileChannel] and [FileLock] objects
 */
internal class Lock(val file: Path) : AutoCloseable {
    private var lockChannel: FileChannel? = null
    private var lock: FileLock? = null

    val isLocked: Boolean
        get() = lockChannel != null

    /**
     * Attempts to lock the file in do-while loop.
     * Does not throw.
     */
    fun loopLock(): LockResult {
        while (true) when (val lock = lock()) {
            LockResult.Success -> return lock
            is LockResult.UnableCreateLock -> return lock
            is LockResult.LockFailed -> {
                /* no-op */
            }
        }
    }

    /**
     * Attempts to lock the file
     * @return returns [LockResult.Success] if successful, otherwise return [LockResult.LockFailed]
     */
    fun lock(): LockResult {
        if (!Files.exists(file.parent, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.createDirectories(file.parent)
            } catch (ex: IOException) {
                return LockResult.UnableCreateLock(ex.message ?: "Failed to create `${file.parent}`")
            }
        }
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) {
            try {
                Files.createFile(file)
            } catch (ex: IOException) {
                return LockResult.UnableCreateLock(ex.message ?: "Failed to create `$file`")
            }
        }

        try {
            val channel = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE)
            val tryLock: FileLock? = channel.tryLock() ?: return LockResult.LockFailed

            lockChannel = channel
            lock = tryLock

            return LockResult.Success
        } catch (ex: IOException) {
            return LockResult.UnableCreateLock(ex.message ?: "Unable to lock $file")
        } catch (ex: OverlappingFileLockException) {
            return LockResult.LockFailed
        }
    }

    /**
     * Unlocks the resources.
     * Does not throw.
     */
    fun unlock(): Unit {
        try {
            lockChannel?.close()
            lockChannel = null

            lock?.release()
            lock = null

            file.toFile().delete()
        } catch (t: IOException) {
        }
    }

    override fun close() {
        unlock()
    }
}