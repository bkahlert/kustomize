package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OS
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import koodies.docker.DockerRequiring
import koodies.io.path.hasContent
import koodies.io.path.textContent
import koodies.junit.UniqueId
import koodies.test.FifteenMinutesTimeout
import koodies.test.HtmlFixture
import koodies.test.TextFixture
import koodies.test.withTempDir
import koodies.text.LineSeparators.LF
import koodies.text.LineSeparators.withTrailingLineSeparator
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.assertions.endsWith

class AppendToFilesPatchTest {

    @FifteenMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
    fun `should create create one append line option for each line`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) =
        withTempDir(uniqueId) {
            val sampleHtmlDiskPath = LinuxRoot.boot / "sample.html"
            val sampleTxtDiskPath = LinuxRoot.etc / "sample.txt"
            val configTxtDiskPath = LinuxRoot.boot.config_txt
            val appendToFilesPatch = AppendToFilesPatch(
                HtmlFixture.text to sampleHtmlDiskPath,
                TextFixture.text to sampleTxtDiskPath,
                "some text" to configTxtDiskPath,
            )

            osImage.patch(appendToFilesPatch)

            expect {
                that(osImage).mounted {
                    path(sampleHtmlDiskPath) { hasContent(HtmlFixture.text.withTrailingLineSeparator()) }
                    path(sampleTxtDiskPath) { hasContent(TextFixture.text.withTrailingLineSeparator()) }
                    path(configTxtDiskPath) { textContent.endsWith("${LF}some text$LF") }
                }
            }
        }
}
