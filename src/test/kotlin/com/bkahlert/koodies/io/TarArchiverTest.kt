package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.PathFixtures.archiveWithTwoFiles
import com.bkahlert.koodies.io.PathFixtures.directoryWithTwoFiles
import com.bkahlert.koodies.io.TarArchiver.tar
import com.bkahlert.koodies.io.TarArchiver.untar
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
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.isGreaterThan

@Execution(CONCURRENT)
class TarArchiverTest {
    @ConcurrentTestFactory
    fun `should throw on missing source`() = listOf(
        { Paths.tempDir().also { it.delete() }.tar() },
        { Paths.tempFile(extension = ".tar").also { it.delete() }.untar() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should throw on non-empty destination`() = listOf(
        { directoryWithTwoFiles().also { it.addExtension("tar").touch().writeText("content") }.tar() },
        { archiveWithTwoFiles("tar").also { it.copyTo(it.removeExtension("tar")) }.untar() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should overwrite non-empty destination`() = listOf(
        { directoryWithTwoFiles().also { it.addExtension("tar").touch().writeText("content") }.tar(overwrite = true) },
        { archiveWithTwoFiles("tar").also { it.copyTo(it.removeExtension("tar")) }.untar(overwrite = true) },
    ).map { call ->
        dynamicTest("$call") {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should tar and untar`() {
        val dir = directoryWithTwoFiles()

        val archivedDir = dir.tar()
        expectThat(archivedDir.size).isGreaterThan(dir.size)

        val renamedDir = dir.renameTo("${dir.fileName}-renamed")

        val unarchivedDir = archivedDir.untar()
        expectThat(unarchivedDir).hasSameFiles(renamedDir)
    }
}
