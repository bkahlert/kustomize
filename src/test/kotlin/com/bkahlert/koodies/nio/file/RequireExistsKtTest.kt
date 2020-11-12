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
class RequireExistsKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should not throw if exists`() {
        tempDir.tempDir().requireExists()
    }

    @Test
    fun `should throw if not exists`() {
        expectCatching { tempDir.tempDir().also { it.delete() }.requireExists() }.isFailure().isA<IllegalArgumentException>()
    }
}
