package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.PathFixtures.archiveWithTwoFiles
import com.bkahlert.koodies.io.PathFixtures.directoryWithTwoFiles
import com.bkahlert.koodies.io.TarArchiver.tar
import com.bkahlert.koodies.io.TarArchiver.untar
import com.bkahlert.koodies.nio.file.addExtension
import com.bkahlert.koodies.nio.file.copyTo
import com.bkahlert.koodies.nio.file.removeExtension
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempPath
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.unit.Size.Companion.size
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.hasSameFiles
import com.imgcstmzr.util.renameTo
import com.imgcstmzr.util.touch
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan
import java.nio.file.FileAlreadyExistsException

@Execution(CONCURRENT)
class TarArchiverTest {

    private val tempDir = tempDir().deleteOnExit()

    @TestFactory
    fun `should throw on missing source`() = listOf(
        { tempDir.tempDir().tempPath().tar() },
        { tempDir.tempDir().tempPath(extension = ".tar").untar() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<FileAlreadyExistsException>()
        }
    }

    @TestFactory
    fun `should throw on non-empty destination`() = listOf(
        { tempDir.tempDir().directoryWithTwoFiles().apply { addExtension("tar").touch().writeText("content") }.tar() },
        { tempDir.tempDir().archiveWithTwoFiles("tar").apply { copyTo(removeExtension("tar")) }.untar() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<FileAlreadyExistsException>()
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`() = listOf(
        { tempDir.tempDir().directoryWithTwoFiles().apply { addExtension("tar").touch().writeText("content") }.tar(overwrite = true) },
        { tempDir.tempDir().archiveWithTwoFiles("tar").apply { copyTo(removeExtension("tar")) }.untar(overwrite = true) },
    ).map { call ->
        dynamicTest("$call") {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should tar and untar`() {
        val dir = tempDir.directoryWithTwoFiles()

        val archivedDir = dir.tar()
        expectThat(archivedDir.size).isGreaterThan(dir.size)

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.untar()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}
