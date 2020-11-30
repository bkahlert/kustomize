package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.test.junit.test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class WithExtensionKtTest {

    @TestFactory
    fun `should add extension if none exists`() = listOf(
        "filename" to "filename.test",
        "my/path/filename" to "my/path/filename.test",
        "/my/path/filename" to "/my/path/filename.test",
    ).test("{} should be {}") { (path, expected) ->
        expectThat(Path.of(path).withExtension("test")).isEqualTo(Path.of(expected))
    }

    @TestFactory
    fun `should replace extension if one exists`() = listOf(
        "filename.pdf" to "filename.test",
        "my/path/filename.pdf" to "my/path/filename.test",
        "/my/path/filename.pdf" to "/my/path/filename.test",
    ).test("{} should be {}") { (path, expected) ->
        expectThat(Path.of(path).withExtension("test")).isEqualTo(Path.of(expected))
    }
}
