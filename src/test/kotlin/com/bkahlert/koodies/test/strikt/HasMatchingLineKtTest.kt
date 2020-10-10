package com.bkahlert.koodies.test.strikt

import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
internal class HasMatchingLineKtTest {
    @Test
    internal fun `matches path with one matching line`() {
        val path = Paths.tempFile().also { it.writeText("abc\nadc\naop\n") }
        expectThat(path).hasMatchingLine("a{}c")
    }

    @Test
    internal fun `matches path with multiple matching line`() {
        val path = Paths.tempFile().also { it.writeText("abc\nadc\naop\n") }
        expectThat(path).hasMatchingLine("a{}")
    }

    @Test
    internal fun `matches path with no matching line`() {
        val path = Paths.tempFile().also { it.writeText("abc\nadc\naop\n") }
        expectThat(path).not { hasMatchingLine("xyz") }
    }
}
