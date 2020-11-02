package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.ClassPath
import com.imgcstmzr.util.Paths
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

    @Nested
    inner class WithFile {
        @Test
        fun `should throw on empty`() {
            expectCatching { Paths.tempFile().requireNotEmpty() }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should not throw on non-empty`() {
            ClassPath("example.html").copyToTempFile().requireNotEmpty()
        }
    }

    @Nested
    inner class WithDirectory {
        @Test
        fun `should throw on empty`() {
            expectCatching { Paths.tempDir().requireNotEmpty() }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should not throw on non-empty`() {
            Paths.tempDir().parent.requireNotEmpty()
        }
    }

    @Test
    fun `should throw on missing`() {
        expectCatching { Paths.tempFile().also { it.delete() }.requireNotEmpty() }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should throw in different type`() {
        @Suppress("BlockingMethodInNonBlockingContext")
        (expectCatching { Files.createSymbolicLink(Paths.tempFile().also { it.delete() }, Paths.tempFile()).requireNotEmpty() })
    }
}
