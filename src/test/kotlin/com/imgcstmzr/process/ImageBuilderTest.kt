package com.imgcstmzr.process

import com.bkahlert.koodies.io.TarArchiveGzCompressor.tarGzip
import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.test.strikt.hasSize
import com.bkahlert.koodies.unit.Gibi
import com.bkahlert.koodies.unit.Mebi
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.bytes
import com.bkahlert.koodies.unit.size
import com.imgcstmzr.process.ImageBuilder.FileSystemType.EXT4
import com.imgcstmzr.process.ImageBuilder.FileSystemType.FAT
import com.imgcstmzr.process.ImageBuilder.buildFrom
import com.imgcstmzr.process.ImageBuilder.format
import com.imgcstmzr.process.ImageBuilder.toPartitions
import com.imgcstmzr.process.ImageBuilder.toSectors
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.addExtension
import com.imgcstmzr.util.appendText
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.mkdirs
import com.imgcstmzr.util.readAll
import com.imgcstmzr.util.removeExtension
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
class ImageBuilderTest {

    @Nested
    inner class Format {

        @ConcurrentTestFactory
        fun `should format size`() = listOf(
            4.Mebi.bytes to "4M",
            4.2.Mebi.bytes to "5M",
            1.5.Gibi.bytes to "1536M",
        ).map { (size, sectors) ->
            dynamicTest("$sectors ≟ $size") {
                expectThat(size.format()).isEqualTo(sectors)
            }
        }
    }

    @Nested
    inner class ToSectors {
        @ConcurrentTestFactory
        fun `should calculate sectors`() = listOf(
            4.Mebi.bytes to 8192,
            4.2.Mebi.bytes to 10240,
            1.5.Gibi.bytes to 3145728,
        ).map { (size, sectors) ->
            dynamicTest("$sectors ≟ $size") {
                expectThat(size.toSectors()).isEqualTo(sectors)
            }
        }
    }

    @Nested
    inner class ToPartitions {

        @ConcurrentTestFactory
        fun `should calculate partitions`() = listOf(
            4.Mebi.bytes to doubleArrayOf(.5, .5) to listOf(2048..4095, 4096..6143),
            4.2.Mebi.bytes to doubleArrayOf(1.0) to listOf(2048..8191),
            1.5.Gibi.bytes to doubleArrayOf(.2, .4, .4) to listOf(2048..630373, 630374..1887026, 1887027..3143679),
        ).map { (sizeToSectors: Pair<Size, DoubleArray>, partitions: List<IntRange>) ->
            dynamicTest("$partitions ≟ ${sizeToSectors.first} / ${sizeToSectors.second.toList()}") {
                val ratio = sizeToSectors.second.first()
                val ratios = sizeToSectors.second.drop(1).toDoubleArray()
                expectThat(sizeToSectors.first.toPartitions(ratio = ratio, ratios = ratios)).isEqualTo(partitions)
            }
        }

        @ConcurrentTestFactory
        fun `should require positive partitions`() = listOf(
            4.Mebi.bytes to doubleArrayOf(.5, -.5),
            4.2.Mebi.bytes to doubleArrayOf(-1.0),
            1.5.Gibi.bytes to doubleArrayOf(.2, -.4, .4),
        ).map { (size: Size, ratios: DoubleArray) ->
            dynamicTest("$size ≟ ${ratios.toList()}") {
                val ratio = ratios.first()
                val ratiosButFirst = ratios.drop(1).toDoubleArray()
                expectCatching { (size.toPartitions(ratio = ratio, ratios = ratiosButFirst)) }
                    .isFailure().isA<java.lang.IllegalArgumentException>()
            }
        }
    }

    @Nested
    inner class BuildFrom {

        @ConcurrentTestFactory
        fun `should require positive partitions`(logger: InMemoryLogger<Path>) = listOf(
            4.Mebi.bytes to doubleArrayOf(.5, -.5),
            4.2.Mebi.bytes to doubleArrayOf(-1.0),
            1.5.Gibi.bytes to doubleArrayOf(.2, -.4, .4),
        ).map { (size: Size, ratios: DoubleArray) ->
            dynamicTest("$size ≟ ${ratios.toList()}") {
                val ratio = ratios.first()
                val ratiosButFirst = ratios.drop(1).toDoubleArray()
                expectCatching {
                    buildFrom(Path.of("/Users/bkahlert/.imgcstmzr.test/imgcstmzr2954831793874525391"),
                        logger = logger,
                        ratio = ratio to FAT,
                        ratios = ratiosButFirst.map { it to EXT4 }.toTypedArray())
                }
                    .isFailure().isA<java.lang.IllegalArgumentException>()
            }
        }

        @Test
        fun `should only accept tar gzip archive`(logger: InMemoryLogger<Path>) {
            expectCatching { buildFrom(Path.of("archive.zip"), logger = logger) }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should only accept two partitions`(logger: InMemoryLogger<Path>) {
            expectCatching { buildFrom(Path.of("archive.tar.gz"), logger = logger, ratios = emptyArray()) }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should only accept EXT4 as first partition`(logger: InMemoryLogger<Path>) {
            expectCatching { buildFrom(Path.of("archive.tar.gz"), logger = logger, ratio = 0.5 to FAT) }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        fun `should only accept FAT as boot partition`(logger: InMemoryLogger<Path>) {
            expectCatching { buildFrom(Path.of("archive.tar.gz"), logger = logger, ratios = arrayOf(0.5 to EXT4)) }.isFailure().isA<IllegalArgumentException>()
        }

        @DockerRequired
        @Test
        fun `should build img from archive`(logger: InMemoryLogger<Path>) {
            val archive = Paths.tempDir().apply {
                resolve("cmdline.txt").apply { writeText("console=serial0,115200 console=tty1 ...") }
                resolve("boot").mkdirs()
                resolve("boot/important.file").apply { writeText("important content") }
                while (size < 4.Mebi.bytes) resolve("fill.txt").appendText(ClassPath("Journey to the West - Introduction.txt").readAll())
            }.tarGzip()

            val img = buildFrom(archive, logger = logger, freeSpaceRatio = 0.20).deleteOnExit()

            expectThat(img).endsWith(archive.removeExtension("tar.gz").addExtension("img")).hasSize(6_291_456.bytes)
        }
    }
}
