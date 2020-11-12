package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.GzCompressor.gunzip
import com.bkahlert.koodies.io.GzCompressor.gzip
import com.bkahlert.koodies.io.PathFixtures.archiveWithSingleFile
import com.bkahlert.koodies.io.PathFixtures.singleFile
import com.bkahlert.koodies.nio.file.requireNotEmpty
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.addExtension
import com.imgcstmzr.util.copyTo
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.hasEqualContent
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
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isLessThan
import java.nio.file.Path

@Execution(CONCURRENT)
class GzCompressorTest {

    private val tempDir = tempDir().deleteOnExit()

    @ConcurrentTestFactory
    fun `should throw on missing source`() = listOf(
        { tempDir.tempFile().also { it.delete() }.gzip() },
        { tempDir.tempFile(extension = ".gz").also { it.delete() }.gunzip() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should throw on non-empty destination`() = listOf(
        { tempDir.singleFile().also { it.addExtension("gz").deleteOnExit().touch().writeText("content") }.gzip() },
        { tempDir.archiveWithSingleFile("gz").also { it.copyTo(it.removeExtension("gz")).deleteOnExit() }.gunzip() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should overwrite non-empty destination`() = listOf(
        { tempDir.singleFile().also { it.addExtension("gz").touch().deleteOnExit().writeText("content") }.gzip(overwrite = true) },
        { tempDir.archiveWithSingleFile("gz").also { it.copyTo(it.removeExtension("gz")).deleteOnExit() }.gunzip(overwrite = true) },
    ).map { call ->
        dynamicTest("$call") {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should gzip and gunzip`() {
        val file: Path = tempDir.singleFile()
        file.requireNotEmpty()

        val gzippedFile = file.gzip()
        expectThat(gzippedFile.size).isLessThan(file.size)

        val renamedFile = file.renameTo("example-${String.random()}.html")

        val gunzippedFile = gzippedFile.gunzip()
        gunzippedFile.requireNotEmpty()
        expectThat(gunzippedFile).isEqualTo(file).hasEqualContent(renamedFile)
    }
}
