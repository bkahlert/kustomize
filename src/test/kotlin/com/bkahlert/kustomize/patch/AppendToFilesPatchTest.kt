package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.LibguestfsImage
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import com.bkahlert.kommons.docker.DockerRequiring
import com.bkahlert.kommons.io.path.hasContent
import com.bkahlert.kommons.io.path.textContent
import com.bkahlert.kommons.junit.UniqueId
import com.bkahlert.kommons.test.FifteenMinutesTimeout
import com.bkahlert.kommons.test.HtmlFixture
import com.bkahlert.kommons.test.TextFixture
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.LineSeparators.LF
import com.bkahlert.kommons.text.LineSeparators.withTrailingLineSeparator
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.endsWith
import strikt.assertions.isEqualTo

class AppendToFilesPatchXTest {

    @Test
    fun `should not exit on error`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val patch = AppendToFilesPatch("test" to LinuxRoot / "test.txt")
        expectThat(patch(osImage)).guestfishCommands {
            any { any { isEqualTo("-write-append") } }
        }
    }

    @Test
    fun `should hex encode lines`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val patch = AppendToFilesPatch(TextFixture.text to LinuxRoot / "test.txt")
        expectThat(patch(osImage)).guestfishCommands {
            any { any { isEqualTo(""""\x61\xc2\x85\xf0\x9d\x95\x93\x0d\x0a\xe2\x98\xb0\x0a\xf0\x9f\x91\x8b\x0a\x0a"""") } }
        }
    }

    @FifteenMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
    fun `should append lines`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) =
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

            expectThat(osImage).mounted {
                path(sampleHtmlDiskPath) { hasContent(HtmlFixture.text.withTrailingLineSeparator()) }
                path(sampleTxtDiskPath) { hasContent(TextFixture.text + LF) }
                path(configTxtDiskPath) { textContent.endsWith("${LF}some text$LF") }
            }
        }
}
