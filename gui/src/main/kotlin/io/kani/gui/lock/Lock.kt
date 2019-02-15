package io.kani.gui.lock

import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.*

/**
 * Internal wrapper around file object for automatic management of [FileChannel] and [FileLock] objects
 */
internal class Lock(val file: Path) : AutoCloseable {
    private var lockChannel: FileChannel? = null
    private var lock: FileLock? = null

    /**
     * Attempts to lock the file in do-while loop.
     * Does not throw.
     */
    fun loopLock() {
        loop@ while (true) when (lock()) {
            LockResult.Success -> return
            is LockResult.AlreadyLocked -> continue@loop
        }
    }

    /**
     * Attempts to lock the file
     * @return returns [LockResult.Success] if successful, otherwise return [LockResult.AlreadyLocked]
     */
    fun lock(): LockResult {
        try {
            if (!Files.exists(file.parent, LinkOption.NOFOLLOW_LINKS)) Files.createDirectories(file.parent)
            if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) Files.createFile(file)

            val channel = FileChannel.open(file, StandardOpenOption.READ, StandardOpenOption.WRITE)

            val tryLock: FileLock? =
                channel.tryLock() ?: return LockResult.AlreadyLocked("Unable to lock $file")

            lockChannel = channel
            lock = tryLock

            return LockResult.Success
        } catch (t: Throwable) {
            return LockResult.AlreadyLocked(t.message ?: "Unable to lock $file")
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