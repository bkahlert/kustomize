package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.PathFixtures.archiveWithTwoFiles
import com.bkahlert.koodies.io.PathFixtures.directoryWithTwoFiles
import com.bkahlert.koodies.io.TarArchiveGzCompressor.listArchive
import com.bkahlert.koodies.io.TarArchiveGzCompressor.tarGunzip
import com.bkahlert.koodies.io.TarArchiveGzCompressor.tarGzip
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.util.Paths
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
import strikt.assertions.isLessThan

@Execution(CONCURRENT)
class TarArchiveGzCompressorTest {
    @ConcurrentTestFactory
    fun `should throw on missing source`() = listOf(
        { Paths.tempDir().also { it.delete() }.tarGzip() },
        { Paths.tempFile(extension = ".tar.gz").also { it.delete() }.tarGunzip() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should throw on non-empty destination`() = listOf(
        { directoryWithTwoFiles().also { it.addExtension("tar.gz").touch().writeText("content") }.tarGzip() },
        { archiveWithTwoFiles("tar.gz").also { it.copyTo(it.removeExtension("tar.gz")) }.tarGunzip() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should overwrite non-empty destination`() = listOf(
        { directoryWithTwoFiles().also { it.addExtension("tar.gz").touch().writeText("content") }.tarGzip(overwrite = true) },
        { archiveWithTwoFiles("tar.gz").also { it.copyTo(it.removeExtension("tar.gz")) }.tarGunzip(overwrite = true) },
    ).map { call ->
        dynamicTest("$call") {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should tar-gzip listArchive and untar-gunzip`() {
        val dir = directoryWithTwoFiles()

        val archivedDir = dir.tarGzip()
        expectThat(archivedDir.size).isLessThan(dir.size)

        expectThat(archivedDir.listArchive().map { it.name }).containsExactlyInAnyOrder("example.html", "sub-dir/", "sub-dir/config.txt")

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.tarGunzip()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}

