package com.bkahlert.kustomize.patch

import com.bkahlert.kommons.docker.DockerRequiring
import com.bkahlert.kommons.exception.rootCause
import com.bkahlert.kommons.io.copyTo
import com.bkahlert.kommons.io.copyToTemp
import com.bkahlert.kommons.io.path.hasContent
import com.bkahlert.kommons.io.path.randomPath
import com.bkahlert.kommons.io.path.tempDir
import com.bkahlert.kommons.io.path.tempFile
import com.bkahlert.kommons.io.path.textContent
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.junit.UniqueId
import com.bkahlert.kommons.test.FiveMinutesTimeout
import com.bkahlert.kommons.test.HtmlFixture
import com.bkahlert.kommons.test.TextFixture
import com.bkahlert.kommons.test.expectThrows
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kustomize.libguestfs.LibguestfsImage
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.nio.file.NoSuchFileException
import kotlin.io.path.createFile

class CopyFilesPatchTest {

    @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
    fun `should create mkdir and copy-in directory and file`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val sampleTxtDiskPath = LinuxRoot.etc / "sample.txt"
        val sampleHtmlDiskPath = LinuxRoot.boot / "sample.html"
        val configTxtDiskPath = LinuxRoot.boot.config_txt
        val copyFilesPatch = CopyFilesPatch(
            { TextFixture.copyToTemp() } to sampleTxtDiskPath,
            {
                tempDir().also {
                    HtmlFixture.copyTo(it.resolve(sampleHtmlDiskPath.fileName))
                    it.resolve(configTxtDiskPath.fileName).writeText("new file")
                }
            } to LinuxRoot.boot,
        )

        osImage.patch(copyFilesPatch)

        expect {
            that(osImage).mounted {
                path(sampleTxtDiskPath) { hasContent(TextFixture.text) }
                path(sampleHtmlDiskPath) { hasContent(HtmlFixture.text) }
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
    fun `should not exit on error`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val patch = CopyFilesPatch({ tempFile() } to LinuxRoot / "test.txt")
        expectThat(patch(osImage)).guestfishCommands {
            any { any { isEqualTo("-copy-in") } }
        }
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
