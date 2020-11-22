package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.ImgFixture.Home.User.ExampleHtml
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isFailure
import java.nio.file.DirectoryNotEmptyException
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files

@Execution(CONCURRENT)
class RequireEmptyKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Nested
    inner class WithFile {
        @Test
        fun `should not throw on empty`() {
            tempDir.tempFile().requireEmpty()
        }

        @Test
        fun `should throw on non-empty`() {
            expectCatching { ExampleHtml.copyToDirectory(tempDir).requireEmpty() }.isFailure()
                .isA<FileAlreadyExistsException>()
        }
    }

    @Nested
    inner class WithDirectory {
        @Test
        fun `should not throw on empty`() {
            tempDir.tempDir().requireEmpty()
        }

        @Test
        fun `should throw on non-empty`() {
            expectCatching { tempDir.tempDir().parent.requireEmpty() }.isFailure().isA<DirectoryNotEmptyException>()
        }
    }

    @Test
    fun `should throw on missing`() {
        expectCatching { tempDir.tempPath().requireEmpty() }.isFailure().isA<FileAlreadyExistsException>()
    }

    @Test
    fun `should throw in different type`() {
        @Suppress("BlockingMethodInNonBlockingContext")
        expectCatching {
            Files.createSymbolicLink(tempDir.tempPath(), tempDir.tempFile()).requireEmpty()
        }
    }
}
