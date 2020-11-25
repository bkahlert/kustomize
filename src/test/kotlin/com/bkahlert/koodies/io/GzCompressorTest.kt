package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.GzCompressor.gunzip
import com.bkahlert.koodies.io.GzCompressor.gzip
import com.bkahlert.koodies.io.PathFixtures.archiveWithSingleFile
import com.bkahlert.koodies.io.PathFixtures.singleFile
import com.bkahlert.koodies.nio.file.addExtension
import com.bkahlert.koodies.nio.file.copyTo
import com.bkahlert.koodies.nio.file.removeExtension
import com.bkahlert.koodies.nio.file.requireNotEmpty
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempPath
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.unit.Size.Companion.size
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.hasEqualContent
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
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isLessThan
import java.nio.file.NoSuchFileException
import java.nio.file.Path

@Execution(CONCURRENT)
class GzCompressorTest {

    private val tempDir = tempDir().deleteOnExit()

    @TestFactory
    fun `should throw on missing source`() = listOf(
        { tempDir.tempDir().tempPath().gzip() },
        { tempDir.tempDir().tempPath(extension = ".gz").gunzip() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<NoSuchFileException>()
        }
    }

    @TestFactory
    fun `should throw on non-empty destination`() = listOf(
        { tempDir.tempDir().singleFile().apply { addExtension("gz").touch().writeText("content") }.gzip() },
        { tempDir.tempDir().archiveWithSingleFile("gz").apply { copyTo(removeExtension("gz")) }.gunzip() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<NoSuchFileException>()
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`() = listOf(
        { tempDir.tempDir().singleFile().apply { addExtension("gz").touch().writeText("content") }.gzip(overwrite = true) },
        { tempDir.tempDir().archiveWithSingleFile("gz").apply { copyTo(removeExtension("gz")) }.gunzip(overwrite = true) },
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
