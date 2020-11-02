package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Archiver.archive
import com.bkahlert.koodies.io.Archiver.listArchive
import com.bkahlert.koodies.io.Archiver.unarchive
import com.bkahlert.koodies.io.PathFixtures.directoryWithTwoFiles
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.Paths.tempFile
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.hasSameFiles
import com.imgcstmzr.util.removeExtension
import com.imgcstmzr.util.renameTo
import com.imgcstmzr.util.touch
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isLessThan

@Execution(CONCURRENT)
class ArchiverTarGzTest {
    @ConcurrentTestFactory
    fun `should throw on missing source`() = listOf(
        { Paths.tempDir().also { it.delete() }.archive() },
        { tempFile(extension = ".tar.gz").also { it.delete() }.unarchive() },
        { tempFile(extension = ".tar.gz").also { it.delete() }.listArchive() },
        { tempFile(extension = ".tar.gz").also { it.writeText("crap") }.listArchive() },
    ).map { call ->
        DynamicTest.dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should throw on non-empty destination`() = listOf(
        { Paths.tempDir().also { it.removeExtension("tar.gz").touch().writeText("content") }.archive() },
        { directoryWithTwoFiles().archive(destination = tempFile(extension = ".tar.gz")).unarchive() },
    ).map { call ->
        DynamicTest.dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @Test
    fun `should tar-gzip and untar-gunzip`() {
        val dir = directoryWithTwoFiles()

        val archivedDir = dir.archive()
        expectThat(archivedDir.size).isLessThan(dir.size)

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.unarchive()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}

