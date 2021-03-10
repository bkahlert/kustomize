package com.imgcstmzr.libguestfs.docker

import com.imgcstmzr.libguestfs.docker.ImageBuilder.buildFrom
import com.imgcstmzr.libguestfs.docker.ImageBuilder.format
import com.imgcstmzr.runtime.OperatingSystems
import com.imgcstmzr.test.DockerRequiring
import com.imgcstmzr.test.FiveMinutesTimeout
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.endsWith
import com.imgcstmzr.test.hasSize
import com.imgcstmzr.withTempDir
import koodies.io.compress.TarArchiveGzCompressor.tarGzip
import koodies.io.path.addExtensions
import koodies.io.path.randomDirectory
import koodies.io.path.removeExtensions
import koodies.io.path.writeText
import koodies.logging.InMemoryLogger
import koodies.runtime.deleteOnExit
import koodies.unit.Gibi
import koodies.unit.Mebi
import koodies.unit.bytes
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createDirectories

@Execution(CONCURRENT)
class ImageBuilderTest {

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
            fun InMemoryLogger.`should build img from archive`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val archive = randomDirectory().apply {
                    resolve("boot").createDirectories()
                    resolve("boot/cmdline.txt").apply { writeText("console=serial0,115200 console=tty1 …") }
                    resolve("boot/important.file").apply { writeText("important content") }
                }.tarGzip()

                val img = buildFrom(archive, totalSize = 6.Mebi.bytes, bootSize = 3.Mebi.bytes).deleteOnExit()

                expectThat(img) {
                    endsWith(archive.removeExtensions("tar", "gz").addExtensions("img"))
                    hasSize(6_291_456.bytes)
                }
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
