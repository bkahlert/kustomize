package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.ImgFixture
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isSuccess
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.NoSuchFileException

@Execution(CONCURRENT)
class RequireNotEmptyKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Nested
    inner class WithFile {
        @Test
        fun `should throw on empty`() {
            expectCatching { tempDir.tempFile().requireNotEmpty() }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should not throw on non-empty`() {
            expectCatching { ImgFixture.Home.User.ExampleHtml.copyToDirectory(tempDir).requireNotEmpty() }.isSuccess()
        }
    }

    @Nested
    inner class WithDirectory {
        @Test
        fun `should throw on empty`() {
            expectCatching { tempDir.tempDir().requireNotEmpty() }.isFailure().isA<NoSuchFileException>()
        }

        @Test
        fun `should not throw on non-empty`() {
            tempDir.tempDir().parent.requireNotEmpty()
        }
    }

    @Test
    fun `should throw on missing`() {
        expectCatching {
            tempDir.tempPath().requireNotEmpty()
        }.isFailure().isA<FileAlreadyExistsException>()
    }

    @Test
    fun `should throw in different type`() {
        @Suppress("BlockingMethodInNonBlockingContext")
        expectCatching {
            Files.createSymbolicLink(tempDir.tempPath(), tempDir.tempFile()).requireNotEmpty()
        }.isFailure().isA<NoSuchFileException>()
    }
}
