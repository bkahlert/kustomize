package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.delete
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.exists
import strikt.assertions.isDirectory
import strikt.assertions.startsWith

@Execution(CONCURRENT)
class TempDirKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should create temp directory if called stand-alone`() {
        expectThat(tempDir.tempDir("base", ".test"))
            .exists()
            .isDirectory()
            .get { "${last()}" }.startsWith("base").endsWith(".test")
    }

    @Test
    fun `should create temp directory if called with path receiver`() {
        expectThat(tempDir.tempDir("parent", "dir").tempDir("child", "dir"))
            .exists()
            .isDirectory()
            .compose("parent and child") {
                get { parent }.isDirectory().get { "${last()}" }.startsWith("parent").endsWith("dir")
                get { "${last()}" }.startsWith("child").endsWith("dir")
            } then { if (allPassed) pass() else fail() }
    }

    @Test
    fun `should create temp directory if called with non-existant path receiver`() {
        expectThat(tempDir.tempDir("parent", "dir").also { it.delete() }.tempDir("child", "dir"))
            .exists()
            .isDirectory()
    }
}
