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
class RequireFileKtTest {
    @Test
    fun `should throw on directory`() {
        expectCatching { Paths.tempDir().requireFile() }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should throw on file`() {
        Paths.tempFile().requireFile()
    }

    @Test
    fun `should throw on missing`() {
        expectCatching { Paths.tempFile().also { it.delete() }.requireFile() }.isFailure().isA<IllegalArgumentException>()
    }
}
