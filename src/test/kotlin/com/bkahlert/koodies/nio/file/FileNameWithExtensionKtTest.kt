package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.test.junit.test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class FileNameWithExtensionKtTest {

    @TestFactory
    fun `should add extension if none exists`() = listOf(
        "filename", "filename.test",
        "my/path/filename", "my/path/filename.test",
        "/my/path/filename", "/my/path/filename.test",
        "/my/path/filename.pdf", "/my/path/filename.test",
    ).test("{} should be filename.test") {
        expectThat(Path.of(it).fileNameWithExtension("test")).isEqualTo("filename.test")
    }

    @TestFactory
    fun `should replace extension if one exists`() = listOf(
        "filename.pdf", "filename.test",
        "my/path/filename.pdf", "my/path/filename.test",
        "/my/path/filename.pdf", "/my/path/filename.test",
    ).test("{} should be filename.test") {
        expectThat(Path.of(it).fileNameWithExtension("test")).isEqualTo("filename.test")
    }
}
