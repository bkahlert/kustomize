package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.Paths
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.exists
import strikt.assertions.isDirectory
import strikt.assertions.isEqualTo
import strikt.assertions.startsWith

@Execution(CONCURRENT)
class TempDirKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should create temp directory if called stand-alone`() {
        expectThat(tempDir("base", ".test").deleteOnExit()) {
            exists()
            isDirectory()
            get { fileName.serialized }.startsWith("base").endsWith(".test")
            get { parent }.isEqualTo(Paths.TEMP)
        }
    }

    @Test
    fun `should create temp directory if called with path receiver`() {
        expectThat(tempDir.tempDir("parent", "dir").tempDir("child", "dir")) {
            exists()
            isDirectory()
            get { fileName.serialized }.startsWith("child").endsWith("dir")
            get { parent.fileName.serialized }.startsWith("parent").endsWith("dir")
            get { parent.parent }.isEqualTo(tempDir)
        }
    }

    @Test
    fun `should create temp file if called with non-existent path receiver`() {
        expectThat(tempDir.tempPath("parent", "path").tempDir("child", "dir")) {
            exists()
            get { parent }.exists()
        }
    }
}
