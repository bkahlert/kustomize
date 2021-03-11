package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.Libguestfs.Companion.hostPath
import com.imgcstmzr.libguestfs.guestfish.mounted
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.FiveMinutesTimeout
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.content
import com.imgcstmzr.test.hasContent
import com.imgcstmzr.test.rootCause
import com.imgcstmzr.withTempDir
import koodies.io.path.withDirectoriesCreated
import koodies.io.path.writeText
import koodies.logging.InMemoryLogger
import koodies.test.Fixtures.copyTo
import koodies.test.HtmlFile
import koodies.test.TextFile
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectCatching
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.nio.file.NoSuchFileException

@Execution(CONCURRENT)
class CopyFilesPatchTest {

    // TODO    @DockerRequiring(["bkahlert/libguestfs"])
    @FiveMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should create mkdir and copy-in options`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) =
        withTempDir(uniqueId) {
            val logger = this@`should create mkdir and copy-in options`
            val sampleHtmlDiskPath = DiskPath("/boot/sample.html")
            val sampleTxtDiskPath = DiskPath("/etc/sample.txt")
            val configTxtDiskPath = DiskPath("/boot/config.txt")

            val copyFilesPatch = CopyFilesPatch(
                HtmlFile.copyTo(resolve("sample.html")) to sampleHtmlDiskPath,
                TextFile.copyTo(resolve("sample.txt")) to sampleTxtDiskPath,
                osImage.hostPath(configTxtDiskPath).apply { withDirectoriesCreated().writeText("new file") } to configTxtDiskPath
            )

            copyFilesPatch.patch(osImage, logger)

            expect {
                that(osImage).mounted(logger) {
                    path(sampleHtmlDiskPath) { hasContent(HtmlFile.text) }
                    path(sampleTxtDiskPath) { hasContent(TextFile.text) }
                    path(configTxtDiskPath) { content.isEqualTo("new file") }
                }
            }
        }

    @Test
    fun `should throw on missing file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectCatching { CopyFilesPatch(resolve("sample.html") to DiskPath("/boot/sample.html")) }
            .isFailure()
            .rootCause
            .isA<NoSuchFileException>()
    }
}
