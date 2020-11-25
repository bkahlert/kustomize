package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.ImgFixture.Home.User.ExampleHtml
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure
import java.nio.file.Files
import java.nio.file.NoSuchFileException
import java.nio.file.Path

@Execution(CONCURRENT)
class IsNotEmptyKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Nested
    inner class WithFile {
        @Test
        fun `should return true on non-empty`() {
            expectThat(ExampleHtml.copyToDirectory(tempDir)).isNotEmpty()
        }

        @Test
        fun `should return false on empty`() {
            expectThat(tempDir.tempFile()).not { isNotEmpty() }
        }
    }

    @Nested
    inner class WithDirectory {
        @Test
        fun `should return true on non-empty`() {
            expectThat(tempDir.tempDir().parent).isNotEmpty()
        }

        @Test
        fun `should return false on empty`() {
            expectThat(tempDir.tempDir()).not { isNotEmpty() }
        }
    }

    @Test
    fun `should throw on missing`() {
        expectCatching {
            tempDir.tempPath().isNotEmpty
        }.isFailure().isA<NoSuchFileException>()
    }

    @Test
    fun `should throw on different type`() {
        @Suppress("BlockingMethodInNonBlockingContext")
        expectCatching {
            Files.createSymbolicLink(tempDir.tempPath(), tempDir.tempFile()).isNotEmpty
        }
    }
}


fun <T : Path> Assertion.Builder<T>.isNotEmpty() =
    assert("is not empty") {
        when (it.isNotEmpty) {
            true -> pass()
            else -> fail("was empty")
        }
    }

