package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Archiver.archive
import com.bkahlert.koodies.io.Archiver.listArchive
import com.bkahlert.koodies.io.Archiver.unarchive
import com.bkahlert.koodies.io.PathFixtures.archiveWithTwoFiles
import com.bkahlert.koodies.io.PathFixtures.directoryWithTwoFiles
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
import strikt.assertions.isLessThan

@Execution(CONCURRENT)
class ArchiverTarGzTest {
    private val tempDir = tempDir().deleteOnExit()

    @ConcurrentTestFactory
    fun `should throw on missing source`() = listOf(
        { tempDir.tempDir().also { it.delete() }.archive() },
        { tempDir.tempFile(extension = ".tar.gz").also { it.delete() }.unarchive() },
        { tempDir.tempFile(extension = ".tar.gz").also { it.delete() }.listArchive() },
        { tempDir.tempFile(extension = ".tar.gz").also { it.writeText("crap") }.listArchive() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should throw on non-empty destination`() = listOf(
        { tempDir.directoryWithTwoFiles().also { it.addExtension("tar.gz").touch().writeText("content") }.archive("tar.gz") },
        { tempDir.archiveWithTwoFiles("tar.gz").also { it.copyTo(it.removeExtension("tar.gz")) }.unarchive() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should overwrite non-empty destination`() = listOf(
        { tempDir.directoryWithTwoFiles().also { it.addExtension("tar.gz").touch().writeText("content") }.archive("tar.gz", overwrite = true) },
        { tempDir.archiveWithTwoFiles("tar.gz").also { it.copyTo(it.removeExtension("tar.gz")) }.unarchive(overwrite = true) },
    ).map { call ->
        dynamicTest("$call") {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should tar-gzip and untar-gunzip`() {
        val dir = tempDir.directoryWithTwoFiles()

        val archivedDir = dir.archive()
        expectThat(archivedDir.size).isLessThan(dir.size)

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.unarchive()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}

