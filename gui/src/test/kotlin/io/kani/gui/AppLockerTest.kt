package io.kani.gui

import io.kani.gui.lock.AppLocker
import io.kani.gui.lock.LockResult
import io.kani.gui.lock.Response
import org.assertj.core.api.Assertions
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Paths

class AppLockerTest {

    @Test
    fun `Double lock`() {
        val lockDir = Files.createTempDirectory("lock-tests")
        val l1 = AppLocker("sameId", lockDir)
        val l2 = AppLocker("sameId", lockDir)

        val r1 = l1.lock()
        val r2 = l2.lock()

        Assertions.assertThat(r1).isEqualTo(LockResult.Success)
        Assertions.assertThat(r2).isInstanceOf(LockResult.LockFailed::class.java)

        Assertions.assertThat(l1.isLocked).isEqualTo(true)
        Assertions.assertThat(l2.isLocked).isEqualTo(false)
    }

    @Test
    fun `Lock the same object after unlock`() {
        val lockDir = Files.createTempDirectory("lock-tests")
        val l1 = AppLocker("sameId", lockDir)

        val r1 = l1.lock()
        l1.unlock()
        val r2 = l1.lock()

        Assertions.assertThat(r1).isEqualTo(LockResult.Success)
        Assertions.assertThat(r2).isEqualTo(LockResult.Success)

        Assertions.assertThat(l1.isLocked).isEqualTo(true)
    }

    @Test
    fun `Lock other object after unlock`() {
        val lockDir = Files.createTempDirectory("lock-tests")
        val l1 = AppLocker("sameId", lockDir)
        val l2 = AppLocker("sameId", lockDir)

        val r1 = l1.lock()
        l1.unlock()
        val r2 = l2.lock()

        Assertions.assertThat(r1).isEqualTo(LockResult.Success)
        Assertions.assertThat(r2).isEqualTo(LockResult.Success)

        Assertions.assertThat(l1.isLocked).isEqualTo(false)
        Assertions.assertThat(l2.isLocked).isEqualTo(true)
    }

    @Test
    fun `Two different locks`() {
        val lockDir = Files.createTempDirectory("lock-tests")
        val l1 = AppLocker("idOne", lockDir)
        val l2 = AppLocker("idTwo", lockDir)

        val r1 = l1.lock()
        val r2 = l2.lock()

        Assertions.assertThat(r1).isEqualTo(LockResult.Success)
        Assertions.assertThat(r2).isEqualTo(LockResult.Success)

        Assertions.assertThat(l1.isLocked).isEqualTo(true)
        Assertions.assertThat(l2.isLocked).isEqualTo(true)
    }

    @Test
    fun `Message communication works after lock`() {
        val lockDir = Files.createTempDirectory("lock-tests")
        val l1 = AppLocker("sameId", lockDir) { "$it + extra" }
        val l2 = AppLocker("sameId", lockDir) { "$it + other" }

        val r1 = l1.lock()

        val a1 = l1.sendMessage("message")
        val a2 = l2.sendMessage("message")

        Assertions.assertThat(a1).isEqualTo(Response.Answer("message + extra"))
        Assertions.assertThat(a2).isEqualTo(Response.Answer("message + extra"))
    }

    @Test
    fun `Message communication doesn't work after lock`() {
        val lockDir = Files.createTempDirectory("lock-tests")
        val l1 = AppLocker("sameId", lockDir)

        val r1 = l1.lock()
        l1.unlock()

        val a1 = l1.sendMessage("message")

        Assertions.assertThat(a1).isInstanceOf(Response.Failure::class.java)
    }


    @Test
    fun `Message communication works after other lock`() {
        val lockDir = Files.createTempDirectory("lock-tests")
        val l1 = AppLocker("sameId", lockDir) { "$it + extra" }
        val l2 = AppLocker("sameId", lockDir) { "$it + other" }

        val r1 = l1.lock()
        l1.unlock()
        val r2 = l2.lock()

        val a1 = l1.sendMessage("message")
        val a2 = l2.sendMessage("message")

        Assertions.assertThat(a1).isEqualTo(Response.Answer("message + other"))
        Assertions.assertThat(a2).isEqualTo(Response.Answer("message + other"))
    }

    @Test
    fun `Message communication doesn't work without lock`() {
        val lockDir = Files.createTempDirectory("lock-tests")
        val l1 = AppLocker("sameId", lockDir)
        val l2 = AppLocker("sameId", lockDir)


        val a1 = l1.sendMessage("message")
        val a2 = l2.sendMessage("message")

        Assertions.assertThat(a1).isInstanceOf(Response.Failure::class.java)
        Assertions.assertThat(a2).isInstanceOf(Response.Failure::class.java)
    }

    @Test
    fun `Inappropriate directory for lock files`() {
        val invalidPath = Paths.get("Z:/invalid_path")
        val l1 = AppLocker("sameId", invalidPath)
        val r1 = l1.lock()

        Assertions.assertThat(r1).isInstanceOf(LockResult.UnableCreateLock::class.java)
    }
}
