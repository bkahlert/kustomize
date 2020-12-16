package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.file.Paths.Temp
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.exists
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith

@Execution(CONCURRENT)
class TempPathKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should create temp path if called stand-alone`() {
        expectThat(tempPath("base", ".test").deleteOnExit()) {
            not { exists() }
            get { fileName.serialized }.startsWith("base").endsWith(".test")
            get { parent }.isEqualTo(Temp)
        }
    }

    @Test
    fun `should create temp path if called with path receiver`() {
        expectThat(tempDir.tempDir("parent", "path").tempPath("child", "path")) {
            not { exists() }
            get { fileName.serialized }.startsWith("child").endsWith("path")
            get { parent.fileName.serialized }.startsWith("parent").endsWith("path")
            get { parent.parent }.isEqualTo(tempDir)
        }
    }

    @Test
    fun `should not create temp path if called with non-existent path receiver`() {
        expectThat(tempDir.tempPath("parent", "path").tempPath("child", "path")) {
            not { exists() }
            get { parent }.not { exists() }
        }
    }
}
