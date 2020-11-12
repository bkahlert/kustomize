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
class RequireExistsNotKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should throw if exists`() {
        expectCatching { tempDir.tempDir().requireExistsNot() }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should not throw if not exists`() {
        tempDir.tempDir().also { it.delete() }.requireExistsNot()
    }
}
