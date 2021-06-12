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
import koodies.io.path.withDirectoriesCreated
import koodies.io.path.writeText
import koodies.logging.InMemoryLogger
import koodies.test.FiveMinutesTimeout
import koodies.test.HtmlFixture
import koodies.test.TextFixture
import koodies.test.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.nio.file.NoSuchFileException

class CopyFilesPatchTest {

    @DockerRequiring([LibguestfsImage::class])
    @FiveMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should create mkdir and copy-in options`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) =
        withTempDir(uniqueId) {
            val sampleHtmlDiskPath = LinuxRoot.boot / "sample.html"
            val sampleTxtDiskPath = LinuxRoot.etc / "sample.txt"
            val configTxtDiskPath = LinuxRoot.boot / "config.txt"
            val copyFilesPatch = CopyFilesPatch(
                HtmlFixture.copyTo(resolve("sample.html")) to sampleHtmlDiskPath,
                TextFixture.copyTo(resolve("sample.txt")) to sampleTxtDiskPath,
                osImage.hostPath(configTxtDiskPath).apply { withDirectoriesCreated().writeText("new file") } to configTxtDiskPath
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
    fun `should throw on missing file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectCatching { CopyFilesPatch(resolve("sample.html") to LinuxRoot / "boot" / "sample.html") }
            .isFailure()
            .rootCause
            .isA<NoSuchFileException>()
    }
}
