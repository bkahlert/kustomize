package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(CONCURRENT)
class IsDefaultFileSystemKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should return true if default file system`() {
        expectThat(tempDir.tempFile().isDefaultFileSystem).isTrue()
    }

    @Test
    fun `should return false if not default file system`() {
        expectThat(tempDir.tempJarFileSystem().getPath("").isDefaultFileSystem).isFalse()
    }
}
