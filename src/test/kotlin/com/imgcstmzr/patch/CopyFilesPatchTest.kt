package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.Libguestfs.Companion.hostPath
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.CopyInOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.MkdirOption
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.hasContent
import com.imgcstmzr.withTempDir
import koodies.test.Fixtures.copyTo
import koodies.test.HtmlFile
import koodies.test.TextFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectCatching
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isFailure
import java.nio.file.NoSuchFileException

@Execution(CONCURRENT)
class CopyFilesPatchTest {

    @Test
    fun `should create mkdir and copy-in options`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val sampleHtmlDiskPath = DiskPath("/boot/sample.html")
        val sampleTxtDiskPath = DiskPath("/etc/sample.txt")

        val copyFilesPatch = CopyFilesPatch(
            HtmlFile.copyTo(resolve("sample.html")) to sampleHtmlDiskPath,
            TextFile.copyTo(resolve("sample.txt")) to sampleTxtDiskPath,
        )

        expect {
            that(copyFilesPatch).customizations(osImage) {
                containsExactly(
                    MkdirOption(DiskPath("/boot")),
                    CopyInOption(osImage.hostPath(sampleHtmlDiskPath), sampleHtmlDiskPath.parent),
                    MkdirOption(DiskPath("/etc")),
                    CopyInOption(osImage.hostPath(sampleTxtDiskPath), sampleTxtDiskPath.parent),
                )
            }
            that(osImage.hostPath(sampleHtmlDiskPath)) { hasContent(HtmlFile.text) }
            that(osImage.hostPath(sampleTxtDiskPath)) { hasContent(TextFile.text) }
        }
    }

    @Test
    fun `should throw on missing file`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        expectCatching { CopyFilesPatch(resolve("sample.html") to DiskPath("/boot/sample.html")) }
            .isFailure()
            .isA<NoSuchFileException>()
    }
}
