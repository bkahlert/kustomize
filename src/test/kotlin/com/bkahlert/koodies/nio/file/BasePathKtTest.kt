package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.test.junit.test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSameInstanceAs
import java.nio.file.Path

@Execution(CONCURRENT)
class BasePathKtTest {
    @TestFactory
    fun `should return all but the extension`() = listOf(
        Path.of("a/b/c.2") to Path.of("a/b/c"),
        Path.of("a/b.1/c.2") to Path.of("a/b.1/c")
    ).test { (path, expected) ->
        expectThat(path.basePath).isEqualTo(expected)
    }

    @TestFactory
    fun `should return same path if no extension`() = listOf(
        Path.of("a/b/c"),
        Path.of("a/b.1/c")
    ).test {
        expectThat(it.basePath).isSameInstanceAs(it)
    }
}
