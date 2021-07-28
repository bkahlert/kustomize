package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.ImageBuilder.buildFrom
import com.imgcstmzr.libguestfs.ImageBuilder.format
import koodies.docker.DockerRequiring
import koodies.io.compress.TarArchiveGzCompressor.tarGzip
import koodies.io.path.addExtensions
import koodies.io.path.deleteOnExit
import koodies.io.path.removeExtensions
import koodies.io.path.writeText
import koodies.io.randomDirectory
import koodies.junit.UniqueId
import koodies.test.FiveMinutesTimeout
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
        fun `should only accept tar gzip archive`() {
            expectThrows<IllegalArgumentException> { buildFrom(Path.of("archive.zip")) }
        }

        @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
        fun `should build img from archive`(uniqueId: UniqueId) = withTempDir(uniqueId) {
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
}
