package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import koodies.content
import koodies.docker.DockerRequiring
import koodies.exception.rootCause
import koodies.io.copyTo
import koodies.io.path.hasContent
import koodies.io.path.writeText
import koodies.junit.UniqueId
import koodies.test.FiveMinutesTimeout
import koodies.test.HtmlFixture
import koodies.test.TextFixture
import koodies.test.hasElements
import koodies.test.withTempDir
import koodies.tracing.rendering.ReturnValues
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.nio.file.NoSuchFileException
import kotlin.io.path.createFile

class CopyFilesPatchTest {

    @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @E2E @Test
    fun `should create mkdir and copy-in options`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val sampleHtmlDiskPath = LinuxRoot.boot / "sample.html"
        val sampleTxtDiskPath = LinuxRoot.etc / "sample.txt"
        val configTxtDiskPath = LinuxRoot.boot / "config.txt"
        val copyFilesPatch = CopyFilesPatch(
            HtmlFixture.copyTo(resolve("sample.html")) to sampleHtmlDiskPath,
            TextFixture.copyTo(resolve("sample.txt")) to sampleTxtDiskPath,
            resolve(configTxtDiskPath.fileName).writeText("new file") to configTxtDiskPath
        )

        copyFilesPatch.patch(osImage)

        expect {
            that(osImage).mounted {
                path(sampleHtmlDiskPath) { hasContent(HtmlFixture.text) }
                path(sampleTxtDiskPath) { hasContent(TextFixture.text) }
                path(configTxtDiskPath) { content.isEqualTo("new file") }
            }
        }
    }

    @Test
    fun `should throw if file is located in exchange dir`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val copyFilesPatch = CopyFilesPatch(osImage.hostPath(LinuxRoot / "test.txt").also { it.createFile() } to LinuxRoot / "test.txt")
        expectThat(copyFilesPatch.patch(osImage)).isA<ReturnValues<Throwable>>().hasElements(
            { isA<IllegalArgumentException>() }
        )
    }

    @Test
    fun `should throw on missing file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectCatching { CopyFilesPatch(resolve("sample.html") to LinuxRoot / "boot" / "sample.html") }
            .isFailure()
            .rootCause
            .isA<NoSuchFileException>()
    }
}
