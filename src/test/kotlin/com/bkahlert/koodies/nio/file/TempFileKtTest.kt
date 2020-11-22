package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.Paths
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.exists
import strikt.assertions.isEqualTo
import strikt.assertions.isRegularFile
import strikt.assertions.startsWith

@Execution(CONCURRENT)
class TempFileKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should create temp file if called stand-alone`() {
        expectThat(tempFile("base", ".test")) {
            exists()
            isRegularFile()
            get { fileName.serialized }.startsWith("base").endsWith(".test")
            get { parent }.isEqualTo(Paths.TEMP)
        }
    }

    @Test
    fun `should create temp file if called with path receiver`() {
        expectThat(tempDir.tempDir("parent", "dir").tempFile("child", "file")) {
            exists()
            isRegularFile()
            get { fileName.serialized }.startsWith("child").endsWith("file")
            get { parent.fileName.serialized }.startsWith("parent").endsWith("dir")
            get { parent.parent }.isEqualTo(tempDir)
        }
    }

    @Test
    fun `should create temp file if called with non-existent path receiver`() {
        expectThat(tempDir.tempPath("parent", "path").tempFile("child", "file")) {
            exists()
            get { parent }.exists()
        }
    }
}
