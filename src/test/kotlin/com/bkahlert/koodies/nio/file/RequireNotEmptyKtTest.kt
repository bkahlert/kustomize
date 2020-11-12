package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.ClassPath
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.copyToTempFile
import com.imgcstmzr.util.delete
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isFailure
import java.nio.file.Files

@Execution(CONCURRENT)
class RequireNotEmptyKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Nested
    inner class WithFile {
        @Test
        fun `should throw on empty`() {
            expectCatching { tempDir.tempFile().requireNotEmpty() }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should not throw on non-empty`() {
            ClassPath("example.html").copyToTempFile().deleteOnExit().requireNotEmpty()
        }
    }

    @Nested
    inner class WithDirectory {
        @Test
        fun `should throw on empty`() {
            expectCatching { tempDir.tempDir().requireNotEmpty() }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should not throw on non-empty`() {
            tempDir.tempDir().parent.requireNotEmpty()
        }
    }

    @Test
    fun `should throw on missing`() {
        expectCatching { tempDir.tempFile().also { it.delete() }.requireNotEmpty() }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should throw in different type`() {
        @Suppress("BlockingMethodInNonBlockingContext")
        (expectCatching { Files.createSymbolicLink(tempDir.tempFile().also { it.delete() }, tempDir.tempFile()).requireNotEmpty() })
    }
}
