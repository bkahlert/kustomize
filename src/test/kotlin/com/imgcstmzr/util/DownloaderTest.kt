package com.imgcstmzr.util

import com.imgcstmzr.os.OperatingSystemMock
import koodies.junit.UniqueId
import koodies.test.Slow
import koodies.test.expectThrows
import koodies.test.toStringIsEqualTo
import koodies.test.withTempDir
import koodies.unit.bytes
import koodies.unit.hasSize
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.io.FileNotFoundException
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.createFile
import kotlin.io.path.exists

class DownloaderTest {

    private val uri = "https://github.com/NicolasCARPi/example-files/raw/master/example.png"
    private fun Path.getTestImage(): Path = resolve("test.img").apply { if (!exists()) createFile() }
    private fun Path.getTestHandler(): (URI) -> Path = { getTestImage() }
    private fun Path.getDownloader() = Downloader(this, "test" to getTestHandler())

    @Slow @Test
    fun `should download OS`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val os = OperatingSystemMock("example", downloadUrl = uri)
        val path = with(getDownloader()) { os.download() }
        expectThat(path).hasSize(40959.bytes)
    }

    @Test
    fun `should call handler instead of downloading`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(getDownloader().download("test://something")).isEqualTo(getTestImage())
    }

    @Slow @Test
    fun `should download file with explicit protocol`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path = getDownloader().download(uri)
        expectThat(path).hasSize(40959.bytes)
    }

    @Slow @Test
    fun `should download file without explicit protocol`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path = getDownloader().download(uri.removeSuffix("https://"))
        expectThat(path).hasSize(40959.bytes)
    }

    @Slow @Test
    fun `should use server side provided name without query or hash`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path = getDownloader().download("$uri?abc=def#hij")
        expectThat(path.fileName).toStringIsEqualTo("example.png")
    }

    @Slow @Test
    fun `should throw on error`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThrows<FileNotFoundException> { getDownloader().download("#+ü protocol--------/uploads/2017/10/file_example_JPG_100kB.jpg") }
    }
}
