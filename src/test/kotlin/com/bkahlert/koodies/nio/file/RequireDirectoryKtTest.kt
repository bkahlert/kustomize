package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isFailure
import java.nio.file.NotDirectoryException

@Execution(CONCURRENT)
class RequireDirectoryKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should throw on file`() {
        expectCatching { tempDir.tempFile().requireDirectory() }.isFailure().isA<NotDirectoryException>()
    }

    @Test
    fun `should not throw on directory`() {
        tempDir.tempDir().requireDirectory()
    }

    @Test
    fun `should throw on missing`() {
        expectCatching { tempDir.tempPath().requireDirectory() }.isFailure().isA<NotDirectoryException>()
    }
}
