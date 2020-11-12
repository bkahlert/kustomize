package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.PathFixtures.archiveWithTwoFiles
import com.bkahlert.koodies.io.PathFixtures.directoryWithTwoFiles
import com.bkahlert.koodies.io.TarArchiver.tar
import com.bkahlert.koodies.io.TarArchiver.untar
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.addExtension
import com.imgcstmzr.util.copyTo
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.hasSameFiles
import com.imgcstmzr.util.removeExtension
import com.imgcstmzr.util.renameTo
import com.imgcstmzr.util.touch
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan

@Execution(CONCURRENT)
class TarArchiverTest {

    private val tempDir = tempDir().deleteOnExit()

    @ConcurrentTestFactory
    fun `should throw on missing source`() = listOf(
        { tempDir.tempDir().also { it.delete() }.tar() },
        { tempDir.tempFile(extension = ".tar").also { it.delete() }.untar() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should throw on non-empty destination`() = listOf(
        { tempDir.directoryWithTwoFiles().also { it.addExtension("tar").deleteOnExit().touch().writeText("content") }.tar() },
        { tempDir.archiveWithTwoFiles("tar").also { it.copyTo(it.removeExtension("tar")).deleteOnExit() }.untar() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should overwrite non-empty destination`() = listOf(
        { tempDir.directoryWithTwoFiles().also { it.addExtension("tar").deleteOnExit().touch().writeText("content") }.tar(overwrite = true) },
        { tempDir.archiveWithTwoFiles("tar").also { it.copyTo(it.removeExtension("tar")).deleteOnExit() }.untar(overwrite = true) },
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
