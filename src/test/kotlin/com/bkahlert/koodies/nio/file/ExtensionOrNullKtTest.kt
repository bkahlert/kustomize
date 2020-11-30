package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.test.junit.test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import java.nio.file.Path

@Execution(CONCURRENT)
class ExtensionOrNullKtTest {
    @TestFactory
    fun `should return extension`() = listOf(
        Path.of("a/b/c.2") to "2",
        Path.of("a/b.1/c.2-1") to "2-1"
    ).test { (path, expected) ->
        expectThat(path.extensionOrNull).isNotNull().isEqualTo(expected)
    }

    @TestFactory
    fun `should return -1 if not in file name`() = listOf(
        Path.of("a/b/c"),
        Path.of("a/b.1/c")
    ).test {
        expectThat(it.extensionOrNull).isNull()
    }
}
