package com.imgcstmzr.process

import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.number.hasSize
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.test.strikt.isEqualToStringWise
import com.bkahlert.koodies.unit.Size.Companion.bytes
import com.imgcstmzr.runtime.OperatingSystemMock
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.CompletionException

@Execution(CONCURRENT)
class DownloaderTest {

    private val tempDir = tempDir().deleteOnExit()
    private val testImage = tempDir.tempFile("test", ".img")
    private val testHandler: (URI, RenderingLogger) -> Path = { _, _ -> testImage }
    private val downloader = Downloader("test" to testHandler)

    @Slow @Test
    fun InMemoryLogger.`should download OS`() {
        val path = with(downloader) {
            OperatingSystemMock(name = "example", downloadUrl = "https://file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg")
                .download(this@`should download OS`)
        }.deleteOnExit()
        expectThat(path).hasSize(102117.bytes)
    }

    @Test
    fun InMemoryLogger.`should call handler instead of downloading`() {
        expectThat(downloader.download("test://something", this).deleteOnExit()).isEqualTo(testImage)
    }

    @Slow @Test
    fun InMemoryLogger.`should download file with explicit protocol`() {
        val path = downloader.download("https://file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg", this).deleteOnExit()
        expectThat(path).hasSize(102117.bytes)
    }

    @Slow @Test
    fun InMemoryLogger.`should download file without explicit protocol`() {
        val path = downloader.download("file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg", this).deleteOnExit()
        expectThat(path).hasSize(102117.bytes)
    }

    @Slow @Test
    fun InMemoryLogger.`should use server side provided name without query or hash`() {
        val path = downloader.download("https://file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg?abc=def#hij", this).deleteOnExit()
        expectThat(path.fileName).isEqualToStringWise("file_example_JPG_100kB.jpg")
    }

    @Test
    fun InMemoryLogger.`should throw on error`() {
        expectCatching { downloader.download("#+ü protocol--------/uploads/2017/10/file_example_JPG_100kB.jpg", this) }
            .isFailure().isA<CompletionException>()
    }
}
