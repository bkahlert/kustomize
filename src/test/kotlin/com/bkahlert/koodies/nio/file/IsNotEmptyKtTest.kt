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
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isTrue
import java.nio.file.Files

@Execution(CONCURRENT)
internal class IsNotEmptyKtTest {
    @Nested
    inner class WithFile {
        @Test
        internal fun `should return true on non-empty`() {
            expectThat(ClassPath("example.html").copyToTempFile().isNotEmpty).isTrue()
        }

        @Test
        internal fun `should return false on empty`() {
            expectThat(Paths.tempFile().isNotEmpty).isFalse()
        }
    }

    @Nested
    inner class WithDirectory {
        @Test
        internal fun `should return true on non-empty`() {
            expectThat(Paths.tempDir().parent.isNotEmpty).isTrue()
        }

        @Test
        internal fun `should return false on empty`() {
            expectThat(Paths.tempDir().isNotEmpty).isFalse()
        }
    }

    @Test
    internal fun `should throw on missing`() {
        expectCatching { Paths.tempFile().also { it.delete() }.isNotEmpty }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    internal fun `should throw in different type`() {
        @Suppress("BlockingMethodInNonBlockingContext")
        (expectCatching { Files.createSymbolicLink(Paths.tempFile().also { it.delete() }, Paths.tempFile()).isNotEmpty })
    }
}

