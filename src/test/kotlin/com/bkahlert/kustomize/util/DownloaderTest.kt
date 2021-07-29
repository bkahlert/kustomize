package com.bkahlert.kustomize.util

import com.bkahlert.kustomize.os.OperatingSystemMock
import koodies.junit.UniqueId
import koodies.test.Slow
import koodies.test.expectThrows
import koodies.test.toStringIsEqualTo
import koodies.test.withTempDir
import koodies.unit.bytes
import koodies.unit.hasSize
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import java.nio.file.Path

class DownloaderTest {

    private val uri = "https://github.com/NicolasCARPi/example-files/raw/master/example.png"
    private fun Path.getDownloader() = Downloader(this)

    @Slow @Test
    fun `should download OS`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val os = OperatingSystemMock("example", downloadUrl = uri)
        val path = getDownloader().download(os.downloadUrl)
        expectThat(path).hasSize(40959.bytes)
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
        expectThrows<IllegalArgumentException> { getDownloader().download("#+ü protocol--------/uploads/2017/10/file_example_JPG_100kB.jpg") }
    }
}
