package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Compressor.compress
import com.bkahlert.koodies.io.Compressor.decompress
import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.nio.file.requireNotEmpty
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.addExtension
import com.imgcstmzr.util.copyTo
import com.imgcstmzr.util.copyToTempFile
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.hasEqualContent
import com.imgcstmzr.util.removeExtension
import com.imgcstmzr.util.renameTo
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isLessThan
import java.nio.file.Path

@Execution(CONCURRENT)
class CompressorTest {
    @ConcurrentTestFactory
    fun `should throw on missing source`() = listOf(
        { Paths.tempFile().also { it.delete() }.compress() },
        { Paths.tempFile(extension = ".bzip2").also { it.delete() }.decompress() },
    ).map { call ->
        DynamicTest.dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @ConcurrentTestFactory
    fun `should throw on existing destination`() = listOf(
        { Paths.tempFile().also { it.copyTo(it.addExtension("bzip2")) }.compress() },
        { Paths.tempFile(extension = ".bzip2").also { it.copyTo(it.removeExtension("bzip2")) }.decompress() },
    ).map { call ->
        DynamicTest.dynamicTest("$call") {
            expectCatching { call() }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @Test
    fun `should compress and decompress`() {
        val file: Path = ClassPath("example.html").copyToTempFile()
        file.requireNotEmpty()

        val compresspedFile = file.compress()
        expectThat(compresspedFile.size).isLessThan(file.size)

        val renamedFile = file.renameTo("example-${String.random()}.html")

        val decompressedFile = compresspedFile.decompress()
        decompressedFile.requireNotEmpty()
        expectThat(decompressedFile).isEqualTo(file).hasEqualContent(renamedFile)
    }
}

