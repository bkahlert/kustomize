package com.imgcstmzr.guestfish

import com.bkahlert.koodies.io.Archiver.archive
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.imgcstmzr.guestfish.ImageExtractor.extractImage
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.fileNameWithExtension
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.mkdirs
import com.imgcstmzr.util.writeText
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
        val img = tempDir.tempFile(extension = ".img").also { it.writeText("Dummy image") }
        expectThat(img.extractImage { it }).isEqualTo(img)
    }

    @Test
    fun `should extract image on single match`() {
        val dir = tempDir.tempDir()
            .also { it.resolve("some.img").also { it.writeText("img file") } }
            .also { it.resolve("other.file").also { it.writeText("other file") } }
            .archive("zip")
        expectThat(dir.extractImage { it }).endsWith("some.img").hasContent("img file")
    }

    @Test
    fun `should extract largest image on multiple matches`() {
        val dir = tempDir.tempDir()
            .also { it.resolve("a.img").also { it.writeText("small") } }
            .also { it.resolve("b.img").also { it.writeText("the largest among the img files") } }
            .also { it.resolve("c.img").also { it.writeText("also small") } }
            .archive("zip")
        expectThat(dir.extractImage { it }).endsWith("b.img").hasContent("the largest among the img files")
    }

    @Test
    fun `should build image on missing image`() {
        val zipFile = tempDir.tempDir()
            .also { it.resolve("cmdline.txt").also { it.writeText("console=serial0,115200 console=tty1 ...") } }
            .also { it.resolve("boot").mkdirs() }
            .also { it.resolve("boot/important.file").also { it.writeText("important content") } }
            .archive("zip")
        val imageBuilder = { archive: Path -> archive.parent.resolve(zipFile.fileNameWithExtension("img")).also { it.writeText("cryptic stuff") } }
        expectThat(zipFile.extractImage(imageBuilder)).endsWith(zipFile.fileNameWithExtension("img")).hasContent("cryptic stuff")
    }
}
