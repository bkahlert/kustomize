package com.bkahlert.koodies.nio.file

import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.nio.file.Path

@Execution(CONCURRENT)
class AddExtensionKtTest {

    @Test
    fun `should throw on empty extensions`() {
        expectCatching { tempPath("filename").addExtension() }.isFailure().isA<IllegalArgumentException>()
    }

    @TestFactory
    fun `should append single extension`() = listOf(
        "filename" to "filename.test",
        "my/path/filename" to "my/path/filename.test",
        "/my/path/filename" to "/my/path/filename.test",
        "filename.foo" to "filename.foo.test",
        "my/path/filename.foo" to "my/path/filename.foo.test",
        "/my/path/filename.foo" to "/my/path/filename.foo.test",
    ).flatMap { (path, expected) ->
        listOf(
            dynamicTest("$path with appended extension \"test\" should be $expected") {
                expectThat(Path.of(path).addExtension("test")).isEqualTo(Path.of(expected))
            },
        )
    }

    @TestFactory
    fun `should append multiple extensions`() = listOf(
        "filename" to "filename.test.ext",
        "my/path/filename" to "my/path/filename.test.ext",
        "/my/path/filename" to "/my/path/filename.test.ext",
        "filename.foo" to "filename.foo.test.ext",
        "my/path/filename.foo" to "my/path/filename.foo.test.ext",
        "/my/path/filename.foo" to "/my/path/filename.foo.test.ext",
    ).flatMap { (path, expected) ->
        listOf(
            dynamicTest("$path with appended extension \"test.ext\" should be $expected") {
                expectThat(Path.of(path).addExtension("test.ext")).isEqualTo(Path.of(expected))
            },
        )
    }

}
