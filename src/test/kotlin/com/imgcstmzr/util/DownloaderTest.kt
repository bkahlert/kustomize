package com.imgcstmzr.util

import com.imgcstmzr.os.OperatingSystemMock
import koodies.logging.InMemoryLogger
import koodies.logging.RenderingLogger
import koodies.test.Slow
import koodies.test.UniqueId
import koodies.test.expectThrows
import koodies.test.toStringIsEqualTo
import koodies.test.withTempDir
import koodies.text.withoutPrefix
import koodies.unit.bytes
import koodies.unit.hasSize
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.FileNotFoundException
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists

@Execution(CONCURRENT)
class DownloaderTest {

    private val uri = "https://github.com/NicolasCARPi/example-files/raw/master/example.png"
    private fun Path.getTestImage(): Path = resolve("test.img").apply { if (!exists()) createFile() }
    private fun Path.getTestHandler(): (URI, RenderingLogger) -> Path = { _, _ -> getTestImage() }
    private fun Path.getDownloader() = Downloader(this, "test" to getTestHandler())

    @Slow @Test
    fun `should download OS`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
        val os = OperatingSystemMock("example", downloadUrl = uri)
        val path = with(getDownloader()) { os.download(logger) }
        expectThat(path).hasSize(40959.bytes)
    }

    @Test
    fun `should call handler instead of downloading`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
        expectThat(getDownloader().download("test://something", logger)).isEqualTo(getTestImage())
    }

    @Slow @Test
    fun `should download file with explicit protocol`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
        val path = getDownloader().download(uri, logger)
        expectThat(path).hasSize(40959.bytes)
    }

    @Slow @Test
    fun `should download file without explicit protocol`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
        val path = getDownloader().download(uri.withoutPrefix("https://"), logger)
        expectThat(path).hasSize(40959.bytes)
    }

    @Slow @Test
    fun `should use server side provided name without query or hash`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
        val path = getDownloader().download("$uri?abc=def#hij", logger)
        expectThat(path.fileName).toStringIsEqualTo("example.png")
    }

    @Test
    fun `should throw on error`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
        expectThrows<FileNotFoundException> { getDownloader().download("#+ü protocol--------/uploads/2017/10/file_example_JPG_100kB.jpg", logger) }
    }
}
