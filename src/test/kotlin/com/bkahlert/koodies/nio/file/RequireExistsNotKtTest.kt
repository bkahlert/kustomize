package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.delete
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isFailure

@Execution(CONCURRENT)
class RequireExistsNotKtTest {
    @Test
    fun `should throw if exists`() {
        expectCatching { Paths.tempDir().requireExistsNot() }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should not throw if not exists`() {
        Paths.tempDir().also { it.delete() }.requireExistsNot()
    }
}
