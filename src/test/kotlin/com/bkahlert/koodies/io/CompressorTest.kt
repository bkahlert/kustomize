package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Compressor.compress
import com.bkahlert.koodies.io.Compressor.decompress
import com.bkahlert.koodies.io.PathFixtures.archiveWithSingleFile
import com.bkahlert.koodies.io.PathFixtures.singleFile
import com.bkahlert.koodies.nio.file.addExtension
import com.bkahlert.koodies.nio.file.copyTo
import com.bkahlert.koodies.nio.file.removeExtension
import com.bkahlert.koodies.nio.file.requireNotEmpty
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempPath
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.string.withRandomSuffix
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
import java.nio.file.FileAlreadyExistsException
import java.nio.file.NoSuchFileException
import java.nio.file.Path


@Execution(CONCURRENT)
class CompressorTest {

    private val tempDir = tempDir().deleteOnExit()

    @TestFactory
    fun `should throw on missing source`() = listOf(
        { tempDir.tempDir().tempPath().compress() },
        { tempDir.tempDir().tempPath(extension = ".bzip2").decompress() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<NoSuchFileException>()
        }
    }

    @TestFactory
    fun `should throw on non-empty destination`() = listOf(
        { tempDir.tempDir().singleFile().apply { addExtension("bzip2").touch().writeText("content") }.compress("bzip2") },
        { tempDir.tempDir().archiveWithSingleFile("bzip2").apply { copyTo(removeExtension("bzip2")) }.decompress() },
    ).map { call ->
        dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<FileAlreadyExistsException>()
        }
    }

    @TestFactory
    fun `should overwrite non-empty destination`() = listOf(
        { tempDir.tempDir().singleFile().apply { addExtension("bzip2").touch().writeText("content") }.compress("bzip2", overwrite = true) },
        { tempDir.tempDir().archiveWithSingleFile("bzip2").apply { copyTo(removeExtension("bzip2")) }.decompress(overwrite = true) },
    ).map { call ->
        dynamicTest("$call") {
            expectThat(call()).exists()
        }
    }

    @Test
    fun `should compress and decompress`() {
        val file: Path = tempDir.singleFile()
        file.requireNotEmpty()

        val compressedFile = file.compress()
        expectThat(compressedFile.size).isLessThan(file.size)

        val renamedFile = file.renameTo("example".withRandomSuffix() + ".html")

        val decompressedFile = compressedFile.decompress()
        decompressedFile.requireNotEmpty()
        expectThat(decompressedFile).isEqualTo(file).hasEqualContent(renamedFile)
    }
}

