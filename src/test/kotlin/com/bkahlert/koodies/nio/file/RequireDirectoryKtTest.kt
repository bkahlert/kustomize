package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.delete
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isFailure
import java.nio.file.NotDirectoryException

@Execution(CONCURRENT)
class RequireDirectoryKtTest {
    @Test
    fun `should throw on file`() {
        expectCatching { Paths.tempFile().requireDirectory() }.isFailure().isA<NotDirectoryException>()
    }

    @Test
    fun `should not throw on directory`() {
        Paths.tempDir().requireDirectory()
    }

    @Test
    fun `should throw on missing`() {
        expectCatching { Paths.tempDir().also { it.delete() }.requireDirectory() }.isFailure().isA<NotDirectoryException>()
    }
}
