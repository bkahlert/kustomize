package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.test.junit.test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class ExtensionIndexKtTest {
    @TestFactory
    fun `should return index of extension`() = listOf(
        Path.of("a/b/c.2") to 5,
        Path.of("a/b.1/c.2") to 7
    ).test { (path, expected) ->
        expectThat(path.extensionIndex).isEqualTo(expected)
    }

    @TestFactory
    fun `should return -1 if not in file name`() = listOf(
        Path.of("a/b/c"),
        Path.of("a/b.1/c")
    ).test {
        expectThat(it.extensionIndex).isEqualTo(-1)
    }
}
