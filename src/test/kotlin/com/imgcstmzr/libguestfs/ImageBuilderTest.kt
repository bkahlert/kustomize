package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.ImageBuilder.buildFrom
import com.imgcstmzr.libguestfs.ImageBuilder.format
import com.imgcstmzr.os.OperatingSystems
import koodies.docker.DockerRequiring
import koodies.io.compress.TarArchiveGzCompressor.tarGzip
import koodies.io.path.addExtensions
import koodies.io.path.deleteOnExit
import koodies.io.path.randomDirectory
import koodies.io.path.removeExtensions
import koodies.io.path.writeText
import koodies.logging.InMemoryLogger
import koodies.test.FiveMinutesTimeout
import koodies.test.UniqueId
import koodies.test.expectThrows
import koodies.test.testEach
import koodies.test.withTempDir
import koodies.unit.Gibi
import koodies.unit.Mebi
import koodies.unit.bytes
import koodies.unit.hasSize
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createDirectories

class ImageBuilderTest {

    @TestFactory
    fun `should format size`() = testEach(
        4.Mebi.bytes to "4M",
        4.2.Mebi.bytes to "5M",
        1.5.Gibi.bytes to "1536M",
    ) { (size, sectors) ->
        expecting { size.format() } that { isEqualTo(sectors) }
    }

    @Nested
    inner class BuildFrom {

        @Test
        fun InMemoryLogger.`should only accept tar gzip archive`() {
            expectThrows<IllegalArgumentException> { buildFrom(Path.of("archive.zip")) }
        }

        @Nested
        inner class ArchiveBasedImageBuild {

            @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
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

            @Test
            fun InMemoryLogger.`should throw on invalid scheme`() {
                expectThrows<IllegalArgumentException> { buildFrom(URI.create("invalid://build/?files=classpath:config.txt%3Eboot")) }
            }

            @Test
            fun InMemoryLogger.`should throw on invalid host`() {
                expectThrows<IllegalArgumentException> { buildFrom(URI.create("imgcstmzr://invalid/?files=classpath:config.txt%3Eboot")) }
            }

            @Test
            fun InMemoryLogger.`should throw on missing destination`() {
                expectThrows<IllegalArgumentException> { buildFrom(URI.create("invalid://build/?files=classpath:config.txt")) }
            }

            @Test
            fun InMemoryLogger.`should throw on missing files`() {
                expectThrows<IllegalArgumentException> { buildFrom(URI.create("invalid://build/")) }
            }

            @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
            fun InMemoryLogger.`should build img`() {
                val uri = URI.create(OperatingSystems.ImgCstmzrTestOS.downloadUrl)

                val img = buildFrom(uri)

                expectThat(img).hasSize(4_194_304.bytes)
            }
        }
    }
}