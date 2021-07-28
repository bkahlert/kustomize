package com.imgcstmzr.util

import com.imgcstmzr.ImgCstmzr
import koodies.docker.download
import koodies.io.path.copyToDirectory
import koodies.io.useRequiredClassPath
import java.net.URI
import java.nio.file.Path

class Downloader(private val downloadDirectory: Path = ImgCstmzr.Download) {

    private fun String.schemeOrThrow(): String = try {
        URI.create(this).scheme
    } catch (e: Throwable) {
        throw IllegalArgumentException("Invalid URI", e)
    }

    /**
     * Downloads the specified [url].
     *
     * Optionally a [filename] can be provided which is used for logging at places where the [url] would otherwise have been used.
     */
    fun download(url: String, filename: String? = null): Path {
        val scheme = url.schemeOrThrow()
        return if (scheme == "classpath") {
            useRequiredClassPath(url) {
                it.copyToDirectory(downloadDirectory)
            }
        } else {
            downloadDirectory.download(url, filename)
        }
    }
}
