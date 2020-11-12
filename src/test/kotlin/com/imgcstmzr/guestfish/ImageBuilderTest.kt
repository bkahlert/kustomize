package com.imgcstmzr.guestfish

import com.bkahlert.koodies.io.TarArchiveGzCompressor.tarGzip
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.test.junit.FiveMinutesTimeout
import com.bkahlert.koodies.test.strikt.hasSize
import com.bkahlert.koodies.unit.Gibi
import com.bkahlert.koodies.unit.Mebi
import com.bkahlert.koodies.unit.bytes
import com.imgcstmzr.guestfish.ImageBuilder.format
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.util.DockerRequiring
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.addExtension
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.mkdirs
import com.imgcstmzr.util.removeExtension
import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
    inner class BuildFrom {

        @Test
        fun `should only accept tar gzip archive`(logger: InMemoryLogger<Path>) {
            expectCatching { ImageBuilder.buildFrom(Path.of("archive.zip"), logger = logger) }.isFailure().isA<IllegalArgumentException>()
        }

        @Nested
        inner class ArchiveBasedImageBuild {
            @FiveMinutesTimeout @DockerRequiring @Test
            fun `should build img from archive`(logger: InMemoryLogger<Path>) {
                val archive = tempDir.tempDir().apply {
                    resolve("boot").mkdirs()
                    resolve("boot/cmdline.txt").apply { writeText("console=serial0,115200 console=tty1 ...") }
                    resolve("boot/important.file").apply { writeText("important content") }
                }.tarGzip()

                val img = ImageBuilder.buildFrom(archive, logger, totalSize = 6.Mebi.bytes, bootSize = 3.Mebi.bytes).deleteOnExit()

                expectThat(img).endsWith(archive.removeExtension("tar.gz").addExtension("img")).hasSize(6_291_456.bytes)
            }
        }

        @Nested
        inner class UriBasedImageBuild {
            @FiveMinutesTimeout @DockerRequiring @Test
            fun `should build img`(logger: InMemoryLogger<Path>) {
                val uri = URI.create(OperatingSystems.ImgCstmzrTestOS.downloadUrl)

                val img = ImageBuilder.buildFrom(uri, logger)

                expectThat(img).hasSize(4_194_304.bytes)
            }

            @Test
            fun `should throw on invalid scheme`(logger: InMemoryLogger<Path>) {
                expectCatching { ImageBuilder.buildFrom(URI.create("invalid://build/?files=classpath:config.txt%3Eboot"), logger) }
                    .isFailure().isA<IllegalArgumentException>()
            }

            @Test
            fun `should throw on invalid host`(logger: InMemoryLogger<Path>) {
                expectCatching { ImageBuilder.buildFrom(URI.create("imgcstmzr://invalid/?files=classpath:config.txt%3Eboot"), logger) }
                    .isFailure().isA<IllegalArgumentException>()
            }

            @Test
            fun `should throw on missing destination`(logger: InMemoryLogger<Path>) {
                expectCatching { ImageBuilder.buildFrom(URI.create("invalid://build/?files=classpath:config.txt"), logger) }
                    .isFailure().isA<IllegalArgumentException>()
            }

            @Test
            fun `should throw on missing files`(logger: InMemoryLogger<Path>) {
                expectCatching { ImageBuilder.buildFrom(URI.create("invalid://build/"), logger) }
                    .isFailure().isA<IllegalArgumentException>()
            }
        }
    }
}
