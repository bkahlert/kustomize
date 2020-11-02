package com.imgcstmzr.process

import com.bkahlert.koodies.test.strikt.hasSize
import com.bkahlert.koodies.unit.bytes
import com.imgcstmzr.process.Downloader.download
import com.imgcstmzr.runtime.OperatingSystemMock
import com.imgcstmzr.util.isEqualToStringWise
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isFailure

@Execution(ExecutionMode.CONCURRENT)
class DownloaderTest {

    @Test
    fun `should download OS`() {
        val path = OperatingSystemMock(downloadUrl = "https://file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg").download()
        expectThat(path).hasSize(102117.bytes)
    }

    @Test
    fun `should throw on missing OS downloadUri`() {
        expectCatching { OperatingSystemMock().download() }.isFailure().isA<IllegalArgumentException>()
    }

    @Test
    fun `should download file with explicit protocol`() {
        val path = download("https://file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg")
        expectThat(path).hasSize(102117.bytes)
    }

    @Test
    fun `should download file without explicit protocol`() {
        val path = download("file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg")
        expectThat(path).hasSize(102117.bytes)
    }

    @Test
    fun `should use server side provided name without query or hash`() {
        val path = download("https://file-examples-com.github.io/uploads/2017/10/file_example_JPG_100kB.jpg?abc=def#hij")
        expectThat(path.fileName).isEqualToStringWise("file_example_JPG_100kB.jpg")
    }

    @Test
    fun `should throw on error`() {
        expectCatching { download("protocol--------/uploads/2017/10/file_example_JPG_100kB.jpg") }
            .isFailure().isA<IllegalStateException>()
    }
}
