package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class ToPathKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should return regular path`() {
        val path = tempDir.tempFile("file", ".txt")
        expectThat("$path".toPath())
            .isEqualTo(path)
            .not { isA<WrappedPath>() }
    }

    @Test
    fun `should not check existence`() {
        val path = tempDir.tempPath("file", ".txt")
        expectThat("$path".toPath())
            .isEqualTo(path)
            .not { exists() }
    }
}
