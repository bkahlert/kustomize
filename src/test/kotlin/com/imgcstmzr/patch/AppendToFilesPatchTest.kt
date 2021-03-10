package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.guestfish.mounted
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.DockerRequiring
import com.imgcstmzr.test.FiveMinutesTimeout
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.content
import com.imgcstmzr.test.hasContent
import com.imgcstmzr.withTempDir
import koodies.logging.InMemoryLogger
import koodies.test.HtmlFile
import koodies.test.TextFile
import koodies.text.LineSeparators.LF
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.assertions.endsWith

@Execution(CONCURRENT)
class AppendToFilesPatchTest {

    // TODO    @DockerRequiring(["bkahlert/libguestfs"])
    @FiveMinutesTimeout @DockerRequiring @Test
    fun InMemoryLogger.`should create create one append line option for each line2`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) =
        withTempDir(uniqueId) {
            val logger = this@`should create create one append line option for each line2`
            val sampleHtmlDiskPath = DiskPath("/boot/sample.html")
            val sampleTxtDiskPath = DiskPath("/etc/sample.txt")
            val configTxtDiskPath = DiskPath("/boot/config.txt")
            val appendToFilesPatch = AppendToFilesPatch(
                HtmlFile.text to sampleHtmlDiskPath,
                TextFile.text to sampleTxtDiskPath,
                "some text" to configTxtDiskPath,
            )

            appendToFilesPatch.patch(osImage, logger)

            expect {
                that(osImage).mounted(logger) {
                    path(sampleHtmlDiskPath) { hasContent(HtmlFile.text) }
                    path(sampleTxtDiskPath) { hasContent(TextFile.text) }
                    path(configTxtDiskPath) { content.endsWith("${LF}some text$LF") }
                }
            }
        }
}
