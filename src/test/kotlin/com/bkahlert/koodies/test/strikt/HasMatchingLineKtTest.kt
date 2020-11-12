package com.bkahlert.koodies.test.strikt

import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class HasMatchingLineKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `matches path with one matching line`() {
        val path = tempDir.tempFile().also { it.writeText("abc\nadc\naop\n") }
        expectThat(path).hasMatchingLine("a{}c")
    }

    @Test
    fun `matches path with multiple matching line`() {
        val path = tempDir.tempFile().also { it.writeText("abc\nadc\naop\n") }
        expectThat(path).hasMatchingLine("a{}")
    }

    @Test
    fun `matches path with no matching line`() {
        val path = tempDir.tempFile().also { it.writeText("abc\nadc\naop\n") }
        expectThat(path).not { hasMatchingLine("xyz") }
    }
}
