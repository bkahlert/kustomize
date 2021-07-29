package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.ImageExtractor.extractImage
import koodies.io.compress.Archiver.archive
import koodies.io.path.hasContent
import koodies.io.path.writeText
import koodies.io.randomDirectory
import koodies.io.randomFile
import koodies.junit.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class ImageExtractorTest {

    @Test
    fun `should return already extracted image`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val img = randomFile(extension = ".img")
        expectThat(img.extractImage()).isEqualTo(img)
    }

    @Test
    fun `should extract image on single match`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val dir = randomDirectory()
            .apply { resolve("some.img").writeText("img file") }
            .apply { resolve("other.file").writeText("other file") }
            .archive("zip")
        expectThat(dir.extractImage()) {
            endsWith("some.img")
            hasContent("img file")
        }
    }

    @Test
    fun `should extract largest image on multiple matches`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val dir = randomDirectory()
            .apply { resolve("a.img").writeText("small") }
            .apply { resolve("b.img").writeText("the largest among the img files") }
            .apply { resolve("c.img").writeText("also small") }
            .archive("zip")
        expectThat(dir.extractImage()) {
            endsWith("b.img")
            hasContent("the largest among the img files")
        }
    }
}
