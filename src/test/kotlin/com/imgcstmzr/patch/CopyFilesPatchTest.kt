package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OS
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import koodies.docker.DockerRequiring
import koodies.exception.rootCause
import koodies.io.copyToTemp
import koodies.io.path.hasContent
import koodies.io.path.textContent
import koodies.io.path.writeText
import koodies.io.randomPath
import koodies.io.tempFile
import koodies.junit.UniqueId
import koodies.test.FiveMinutesTimeout
import koodies.test.HtmlFixture
import koodies.test.TextFixture
import koodies.test.expectThrows
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.nio.file.NoSuchFileException
import kotlin.io.path.createFile

class CopyFilesPatchTest {

    @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
    fun `should create mkdir and copy-in options`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val sampleHtmlDiskPath = LinuxRoot.boot / "sample.html"
        val sampleTxtDiskPath = LinuxRoot.etc / "sample.txt"
        val configTxtDiskPath = LinuxRoot.boot.config_txt
        val copyFilesPatch = CopyFilesPatch(
            { HtmlFixture.copyToTemp() } to sampleHtmlDiskPath,
            { TextFixture.copyToTemp() } to sampleTxtDiskPath,
            { tempFile("config", ".txt").writeText("new file") } to configTxtDiskPath
        )

        osImage.patch(copyFilesPatch)

        expect {
            that(osImage).mounted {
                path(sampleHtmlDiskPath) { hasContent(HtmlFixture.text) }
                path(sampleTxtDiskPath) { hasContent(TextFixture.text) }
                path(configTxtDiskPath) { textContent.isEqualTo("new file") }
            }
        }
    }

    @Test
    fun `should throw if file is located in exchange dir`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val file = osImage.hostPath(LinuxRoot / "test.txt").also { it.createFile() }
        val patch = CopyFilesPatch({ file } to LinuxRoot / "test.txt")
        expectThrows<IllegalArgumentException> { osImage.patch(patch) }
    }

    @Test
    fun `should throw on missing file`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val file = randomPath()
        val patch = CopyFilesPatch({ file } to LinuxRoot / "boot" / "sample.html")
        expectCatching { osImage.patch(patch) }
            .isFailure()
            .rootCause
            .isA<NoSuchFileException>()
    }
}
