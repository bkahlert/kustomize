package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.test.junit.test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class BaseNameKtTest {
    @TestFactory
    fun `should return only the file name without extension`() = listOf(
        Path.of("a/b/c.2"),
        Path.of("a/b.1/c.2"),
    ).test {
        expectThat(it.baseName).isEqualTo(it.fileSystem.getPath("c"))
    }

    @TestFactory
    fun `should return only the file name even if its already missing`() = listOf(
        Path.of("a/b/c"),
        Path.of("a/b.1/c"),
    ).test {
        expectThat(it.baseName).isEqualTo(it.fileName)
    }
}
