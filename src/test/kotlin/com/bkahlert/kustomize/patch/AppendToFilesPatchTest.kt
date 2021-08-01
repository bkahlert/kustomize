package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.LibguestfsImage
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
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
                    path(sampleTxtDiskPath) { hasContent(koodies.text.LineSeparators.unify(TextFixture.text + LF)) }
                    path(configTxtDiskPath) { textContent.endsWith("${LF}some text$LF") }
                }
            }
        }
}
