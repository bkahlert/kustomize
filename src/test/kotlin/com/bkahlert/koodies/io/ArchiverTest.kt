package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Archiver.archive
import com.bkahlert.koodies.io.Archiver.listArchive
import com.bkahlert.koodies.io.Archiver.unarchive
import com.bkahlert.koodies.io.PathFixtures.archiveWithTwoFiles
import com.bkahlert.koodies.io.PathFixtures.directoryWithTwoFiles
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
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
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isFailure

@Execution(CONCURRENT)
class ArchiverTest {
    private val tempDir = tempDir().deleteOnExit()

    @ConcurrentTestFactory
    fun `should throw on missing source`() = listOf(
        { tempDir.tempDir().also { it.delete() }.archive() },
        { tempDir.tempFile(extension = ".zip").also { it.delete() }.unarchive() },
        { tempDir.tempFile(extension = ".tar.gz").also { it.delete() }.listArchive() },
        { tempDir.tempFile(extension = ".tar.gz").also { it.writeText("crap") }.listArchive() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should throw on non-empty destination`() = listOf(
        { tempDir.directoryWithTwoFiles().also { it.addExtension("zip").touch().writeText("content") }.archive("zip") },
        { tempDir.archiveWithTwoFiles("zip").also { it.copyTo(it.removeExtension("zip")) }.unarchive() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should overwrite non-empty destination`() = listOf(
        { tempDir.directoryWithTwoFiles().also { it.addExtension("zip").touch().writeText("content") }.archive("zip", overwrite = true) },
        { tempDir.archiveWithTwoFiles("zip").also { it.copyTo(it.removeExtension("zip")) }.unarchive(overwrite = true) },
    ).map { call ->
        dynamicTest("$call") {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should archive and unarchive`() {
        val dir = tempDir.directoryWithTwoFiles()

        val archivedDir = dir.archive()

        expectThat(archivedDir.listArchive().map { it.name }).containsExactlyInAnyOrder("example.html", "sub-dir/", "sub-dir/config.txt")

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.unarchive()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}
