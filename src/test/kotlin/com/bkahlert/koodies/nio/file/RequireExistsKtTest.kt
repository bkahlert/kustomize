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
class RequireExistsKtTest {
    @Test
    fun `should not throw if exists`() {
        Paths.tempDir().requireExists()
    }

    @Test
    fun `should throw if not exists`() {
        expectCatching { Paths.tempDir().also { it.delete() }.requireExists() }.isFailure().isA<IllegalArgumentException>()
    }
}
