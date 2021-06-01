package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.ImageExtractor.extractImage
import koodies.io.compress.Archiver.archive
import koodies.io.path.fileNameWithExtension
import koodies.io.path.hasContent
import koodies.io.path.randomDirectory
import koodies.io.path.randomFile
import koodies.io.path.writeText
import koodies.logging.InMemoryLogger
import koodies.test.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path
import kotlin.io.path.createDirectories

class ImageExtractorTest {

    @Test
    fun `should return already extracted image`(logger: InMemoryLogger, uniqueId: UniqueId) = withTempDir(uniqueId) {
        val img = randomFile(extension = ".img").apply { writeText("Dummy image") }
        expectThat(img.extractImage(logger) { it }).isEqualTo(img)
    }

    @Test
    fun `should extract image on single match`(logger: InMemoryLogger, uniqueId: UniqueId) = withTempDir(uniqueId) {
        val dir = randomDirectory()
            .apply { resolve("some.img").writeText("img file") }
            .apply { resolve("other.file").writeText("other file") }
            .archive("zip")
        expectThat(dir.extractImage(logger) { it }) {
            endsWith("some.img")
            hasContent("img file")
        }
    }

    @Test
    fun `should extract largest image on multiple matches`(logger: InMemoryLogger, uniqueId: UniqueId) = withTempDir(uniqueId) {
        val dir = randomDirectory()
            .apply { resolve("a.img").writeText("small") }
            .apply { resolve("b.img").writeText("the largest among the img files") }
            .apply { resolve("c.img").writeText("also small") }
            .archive("zip")
        expectThat(dir.extractImage(logger) { it }) {
            endsWith("b.img")
            hasContent("the largest among the img files")
        }
    }

    @Test
    fun `should build image on missing image`(logger: InMemoryLogger, uniqueId: UniqueId) = withTempDir(uniqueId) {
        val zipFile = randomDirectory()
            .apply { resolve("cmdline.txt").writeText("console=serial0,115200 console=tty1 …") }
            .apply { resolve("boot").createDirectories() }
            .apply { resolve("boot/important.file").writeText("important content") }
            .archive("zip")
        val imageBuilder = { archive: Path ->
            archive.resolveSibling(zipFile.fileNameWithExtension("img")).apply {
                writeText("cryptic stuff")
            }
        }
        expectThat(zipFile.extractImage(logger, imageBuilder)) {
            endsWith(zipFile.fileNameWithExtension("img"))
            hasContent("cryptic stuff")
        }
    }
}