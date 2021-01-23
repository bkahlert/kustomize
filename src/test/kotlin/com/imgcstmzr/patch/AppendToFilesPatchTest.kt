package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.AppendLineOption
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.withTempDir
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.assertions.containsExactly

@Execution(CONCURRENT)
class AppendToFilesPatchTest {

    @Test
    fun `should create create one append line option for each line`(uniqueId: UniqueId, osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val sampleHtmlDiskPath = DiskPath("/boot/sample.html")
        val sampleTxtDiskPath = DiskPath("/etc/sample.txt")

        val appendToFilesPatch = AppendToFilesPatch(
            """
                <p>
                    new paragraph
                </p>
            """.trimIndent() to sampleHtmlDiskPath,
            "some text" to sampleTxtDiskPath,
        )

        expect {
            that(appendToFilesPatch).customizations(osImage) {
                containsExactly(
                    AppendLineOption(sampleHtmlDiskPath, "<p>"),
                    AppendLineOption(sampleHtmlDiskPath, "    new paragraph"),
                    AppendLineOption(sampleHtmlDiskPath, "</p>"),
                    AppendLineOption(sampleTxtDiskPath, "some text"),
                )
            }
        }
    }
}
