package com.imgcstmzr.libguestfs.guestfish

import com.bkahlert.koodies.io.Archiver.archive
import com.bkahlert.koodies.nio.file.fileNameWithExtension
import com.bkahlert.koodies.nio.file.mkdirs
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.nio.file.writeText
import com.imgcstmzr.libguestfs.docker.ImageExtractor.extractImage
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.hasContent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class ImageExtractorTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should return already extracted image`() {
        val img = tempDir.tempFile(extension = ".img").writeText("Dummy image")
        expectThat(img.extractImage { it }).isEqualTo(img)
    }

    @Test
    fun `should extract image on single match`() {
        val dir = tempDir.tempDir()
            .apply { resolve("some.img").writeText("img file") }
            .apply { resolve("other.file").writeText("other file") }
            .archive("zip")
        expectThat(dir.extractImage { it }).endsWith("some.img").hasContent("img file")
    }

    @Test
    fun `should extract largest image on multiple matches`() {
        val dir = tempDir.tempDir()
            .apply { resolve("a.img").writeText("small") }
            .apply { resolve("b.img").writeText("the largest among the img files") }
            .apply { resolve("c.img").writeText("also small") }
            .archive("zip")
        expectThat(dir.extractImage { it }).endsWith("b.img").hasContent("the largest among the img files")
    }

    @Test
    fun `should build image on missing image`() {
        val zipFile = tempDir.tempDir()
            .apply { resolve("cmdline.txt").writeText("console=serial0,115200 console=tty1 ...") }
            .apply { resolve("boot").mkdirs() }
            .apply { resolve("boot/important.file").writeText("important content") }
            .archive("zip")
        val imageBuilder = { archive: Path -> archive.resolveSibling(zipFile.fileNameWithExtension("img")).writeText("cryptic stuff") }
        expectThat(zipFile.extractImage(imageBuilder)).endsWith(zipFile.fileNameWithExtension("img")).hasContent("cryptic stuff")
    }
}
