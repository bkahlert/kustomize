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
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.nio.file.Files

@Execution(CONCURRENT)
class IsNotEmptyKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Nested
    inner class WithFile {
        @Test
        fun `should return true on non-empty`() {
            expectThat(ClassPath("example.html").copyToTempFile().deleteOnExit().isNotEmpty).isTrue()
        }

        @Test
        fun `should return false on empty`() {
            expectThat(tempDir.tempFile().isNotEmpty).isFalse()
        }
    }

    @Nested
    inner class WithDirectory {
        @Test
        fun `should return true on non-empty`() {
            expectThat(tempDir.tempDir().parent.isNotEmpty).isTrue()
        }

        @Test
        fun `should return false on empty`() {
            expectThat(tempDir.tempDir().isNotEmpty).isFalse()
        }
    }

    @Test
    fun `should throw on missing`() {
        expectCatching { tempDir.tempFile().also { it.delete() }.isNotEmpty }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should throw in different type`() {
        @Suppress("BlockingMethodInNonBlockingContext")
        (expectCatching { Files.createSymbolicLink(tempDir.tempFile().also { it.delete() }, tempDir.tempFile()).isNotEmpty })
    }
}

