package com.imgcstmzr.util

import com.imgcstmzr.runtime.OperatingSystemMock
import com.imgcstmzr.test.Slow
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.hasSize
import com.imgcstmzr.test.toStringIsEqualTo
import com.imgcstmzr.withTempDir
import koodies.logging.InMemoryLogger
import koodies.logging.RenderingLogger
import koodies.unit.bytes
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
import kotlin.io.path.createFile
import kotlin.io.path.exists

@Execution(CONCURRENT)
class DownloaderTest {

    private fun Path.getTestImage(): Path = resolve("test.img").apply { if (!exists()) createFile() }
    private fun Path.getTestHandler(): (URI, RenderingLogger) -> Path = { _, _ -> getTestImage() }
    private fun Path.getDownloader() = Downloader(this, "test" to getTestHandler())

    @Slow @Test
    fun InMemoryLogger.`should download OS`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path = with(getDownloader()) {
            OperatingSystemMock(name = "example", downloadUrl = "https://file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg")
                .download(this@`should download OS`)
        }
        expectThat(path).hasSize(102117.bytes)
    }

    @Test
    fun InMemoryLogger.`should call handler instead of downloading`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(getDownloader().download("test://something", this@`should call handler instead of downloading`)).isEqualTo(getTestImage())
    }

    @Slow @Test
    fun InMemoryLogger.`should download file with explicit protocol`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path = getDownloader().download("https://file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg",
            this@`should download file with explicit protocol`)
        expectThat(path).hasSize(102117.bytes)
    }

    @Slow @Test
    fun InMemoryLogger.`should download file without explicit protocol`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path = getDownloader().download("file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg",
            this@`should download file without explicit protocol`)
        expectThat(path).hasSize(102117.bytes)
    }

    @Slow @Test
    fun InMemoryLogger.`should use server side provided name without query or hash`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path = getDownloader().download("https://file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg?abc=def#hij",
            this@`should use server side provided name without query or hash`)
        expectThat(path.fileName).toStringIsEqualTo("file_example_JPG_100kB.jpg")
    }

    @Test
    fun InMemoryLogger.`should throw on error`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectCatching { getDownloader().download("#+ü protocol--------/uploads/2017/10/file_example_JPG_100kB.jpg", this@`should throw on error`) }
            .isFailure().isA<IllegalStateException>()
    }
}
