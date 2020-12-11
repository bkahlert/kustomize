package com.imgcstmzr.guestfish

import com.bkahlert.koodies.io.TarArchiveGzCompressor.tarGzip
import com.bkahlert.koodies.nio.file.addExtension
import com.bkahlert.koodies.nio.file.mkdirs
import com.bkahlert.koodies.nio.file.removeExtension
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.number.hasSize
import com.bkahlert.koodies.test.junit.FiveMinutesTimeout
import com.bkahlert.koodies.unit.Gibi
import com.bkahlert.koodies.unit.Mebi
import com.bkahlert.koodies.unit.Size.Companion.bytes
import com.imgcstmzr.guestfish.ImageBuilder.buildFrom
import com.imgcstmzr.guestfish.ImageBuilder.format
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.util.DockerRequiring
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.net.URI
import java.nio.file.Path

@Execution(CONCURRENT)
class ImageBuilderTest {

    private val tempDir = tempDir().deleteOnExit()

    @Nested
    inner class Format {

        @TestFactory
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
    inner class BuildFrom {

        @Test
        fun InMemoryLogger.`should only accept tar gzip archive`() {
            expectCatching { buildFrom(Path.of("archive.zip")) }.isFailure().isA<IllegalArgumentException>()
        }

        @Nested
        inner class ArchiveBasedImageBuild {
            @FiveMinutesTimeout @DockerRequiring @Test
            fun InMemoryLogger.`should build img from archive`() {
                val archive = tempDir.tempDir().apply {
                    resolve("boot").mkdirs()
                    resolve("boot/cmdline.txt").apply { writeText("console=serial0,115200 console=tty1 ...") }
                    resolve("boot/important.file").apply { writeText("important content") }
                }.tarGzip()

                val img = buildFrom(archive, totalSize = 6.Mebi.bytes, bootSize = 3.Mebi.bytes).deleteOnExit()

                expectThat(img).endsWith(archive.removeExtension("tar.gz").addExtension("img")).hasSize(6_291_456.bytes)
            }
        }

        @Nested
        inner class UriBasedImageBuild {
            @FiveMinutesTimeout @DockerRequiring @Test
            fun InMemoryLogger.`should build img`() {
                val uri = URI.create(OperatingSystems.ImgCstmzrTestOS.downloadUrl)

                val img = buildFrom(uri)

                expectThat(img).hasSize(4_194_304.bytes)
            }

            @Test
            fun InMemoryLogger.`should throw on invalid scheme`() {
                expectCatching { buildFrom(URI.create("invalid://build/?files=classpath:config.txt%3Eboot")) }
                    .isFailure().isA<IllegalArgumentException>()
            }

            @Test
            fun InMemoryLogger.`should throw on invalid host`() {
                expectCatching { buildFrom(URI.create("imgcstmzr://invalid/?files=classpath:config.txt%3Eboot")) }
                    .isFailure().isA<IllegalArgumentException>()
            }

            @Test
            fun InMemoryLogger.`should throw on missing destination`() {
                expectCatching { buildFrom(URI.create("invalid://build/?files=classpath:config.txt")) }
                    .isFailure().isA<IllegalArgumentException>()
            }

            @Test
            fun InMemoryLogger.`should throw on missing files`() {
                expectCatching { buildFrom(URI.create("invalid://build/")) }
                    .isFailure().isA<IllegalArgumentException>()
            }
        }
    }
}
