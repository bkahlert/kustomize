package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.LibguestfsImage
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import koodies.docker.DockerRequiring
import koodies.exception.rootCause
import koodies.io.copyTo
import koodies.io.copyToTemp
import koodies.io.path.hasContent
import koodies.io.path.textContent
import koodies.io.path.writeText
import koodies.io.randomPath
import koodies.io.tempDir
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
