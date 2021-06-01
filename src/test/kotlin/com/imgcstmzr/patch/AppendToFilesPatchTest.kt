package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import koodies.content
import koodies.docker.DockerRequiring
import koodies.io.path.hasContent
import koodies.logging.InMemoryLogger
import koodies.test.FiveMinutesTimeout
import koodies.test.HtmlFixture
import koodies.test.TextFixture
import koodies.test.UniqueId
import koodies.test.withTempDir
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.withTrailingLineSeparator
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.endsWith

class AppendToFilesPatchTest {

    @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @E2E @Test
    fun InMemoryLogger.`should create create one append line option for each line`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) =
        withTempDir(uniqueId) {
            val sampleHtmlDiskPath = DiskPath("/boot/sample.html")
            val sampleTxtDiskPath = DiskPath("/etc/sample.txt")
            val configTxtDiskPath = DiskPath("/boot/config.txt")
            val appendToFilesPatch = AppendToFilesPatch(
                HtmlFixture.text to sampleHtmlDiskPath,
                TextFixture.text to sampleTxtDiskPath,
                "some text" to configTxtDiskPath,
            )

            appendToFilesPatch.patch(osImage)

            expect {
                that(osImage).mounted {
                    path(sampleHtmlDiskPath) { hasContent(HtmlFixture.text.withTrailingLineSeparator()) }
                    path(sampleTxtDiskPath) { hasContent(TextFixture.text.withTrailingLineSeparator()) }
                    path(configTxtDiskPath) { content.endsWith("${LF}some text$LF") }
                }
            }
        }
}
