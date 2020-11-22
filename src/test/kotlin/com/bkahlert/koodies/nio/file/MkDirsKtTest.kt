package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.hasContent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isDirectory
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.nio.file.FileAlreadyExistsException

@Execution(CONCURRENT)
class MkDirsKtTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should create missing directory`() {
        expectThat(tempDir.resolve("sub-dir").mkdirs()).isDirectory()
    }

    @Test
    fun `should return created directory`() {
        val dir = tempDir.tempPath()
        expectThat(dir.mkdirs()).isEqualTo(dir)
    }

    @Test
    fun `should create missing directories`() {
        expectThat(tempDir.resolve("other-dir/sub/sub-sub").mkdirs()).isDirectory()
    }

    @Test
    fun `should create partially missing directories`() {
        val alreadyExistingDir = tempDir.tempDir()
        expectThat(tempDir.resolve("${alreadyExistingDir.fileName}/sub/sub-sub").mkdirs()).isDirectory()
    }

    @Test
    fun `should throw on existing file`() {
        val alreadyExistingFile = tempDir.tempFile().writeText("test")
        expect {
            catching { tempDir.resolve("${alreadyExistingFile.fileName}").mkdirs() }.isFailure().isA<FileAlreadyExistsException>()
            that(alreadyExistingFile).hasContent("test")
        }
    }
}
