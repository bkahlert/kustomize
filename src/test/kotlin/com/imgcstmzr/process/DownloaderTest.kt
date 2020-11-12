package com.imgcstmzr.process

import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.test.junit.Slow
import com.bkahlert.koodies.test.strikt.hasSize
import com.bkahlert.koodies.unit.bytes
import com.imgcstmzr.runtime.OperatingSystemMock
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.isEqualToStringWise
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

@Execution(CONCURRENT)
class DownloaderTest {

    private val tempDir = tempDir().deleteOnExit()
    private val testImage = tempDir.tempFile("test", ".img")
    private val testHandler: (URI, RenderingLogger<*>) -> Path = { _, _ -> testImage }
    private val downloader = Downloader("test" to testHandler)

    @Slow @Test
    fun `should download OS`(logger: InMemoryLogger<*>) {
        val path = with(downloader) {
            OperatingSystemMock(name = "example", downloadUrl = "https://file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg")
                .download(logger)
        }
        expectThat(path).hasSize(102117.bytes)
    }

    @Test
    fun `should call handler instead of downloading`(logger: InMemoryLogger<*>) {
        expectThat(downloader.download("test://something", logger)).isEqualTo(testImage)
    }

    @Slow @Test
    fun `should download file with explicit protocol`(logger: InMemoryLogger<*>) {
        val path = downloader.download("https://file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg", logger)
        expectThat(path).hasSize(102117.bytes)
    }

    @Slow @Test
    fun `should download file without explicit protocol`(logger: InMemoryLogger<*>) {
        val path = downloader.download("file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg", logger)
        expectThat(path).hasSize(102117.bytes)
    }

    @Slow @Test
    fun `should use server side provided name without query or hash`(logger: InMemoryLogger<*>) {
        val path = downloader.download("https://file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg?abc=def#hij", logger)
        expectThat(path.fileName).isEqualToStringWise("file_example_JPG_100kB.jpg")
    }

    @Test
    fun `should throw on error`(logger: InMemoryLogger<*>) {
        expectCatching { downloader.download("#+ü protocol--------/uploads/2017/10/file_example_JPG_100kB.jpg", logger) }
            .isFailure().isA<IllegalStateException>()
    }
}
