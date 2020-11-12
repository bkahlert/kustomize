package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.delete
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isFailure

@Execution(CONCURRENT)
class RequireFileKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should throw on directory`() {
        expectCatching { tempDir.tempDir().requireFile() }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should throw on file`() {
        tempDir.tempFile().requireFile()
    }

    @Test
    fun `should throw on missing`() {
        expectCatching { tempDir.tempFile().also { it.delete() }.requireFile() }.isFailure().isA<IllegalArgumentException>()
    }
}
