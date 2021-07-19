package com.imgcstmzr.util

import com.imgcstmzr.ImgCstmzr
import com.imgcstmzr.os.OperatingSystem
import koodies.docker.download
import koodies.io.path.copyToDirectory
import koodies.io.randomDirectory
import koodies.io.useRequiredClassPath
import java.net.URI
import java.nio.file.Path

/**
 * Utility to retrieve [OperatingSystem] images by downloading them using
 * the corresponding [OperatingSystem.downloadUrl].
 *
 * Also [customHandlerMapping] can be registered—each being responsible for an [URI] schema.
 *
 * In case a matching [customHandlerMapping] is found, no download takes place but the
 * corresponding [Handler] is called to retrieve a copy of the requested image.
 */
class Downloader(
    private val downloadDirectory: Path = ImgCstmzr.Temp,
    vararg customHandlers: Pair<String, Handler> = arrayOf(
        "classpath" to { uri ->
            useRequiredClassPath(uri.toString()) {
                it.copyToDirectory(downloadDirectory)
            }
        }
    ),
) {
    private val customHandlerMapping = customHandlers.toMap()

    private fun String.findScheme() = kotlin.runCatching { URI.create(this).scheme }.getOrElse { IllegalArgumentException("Invalid URI", it) }

    /**
     * Downloads an image copy containing the [OperatingSystem] and returns the [Path] where the
     * copy can be found after successful download.
     */
    fun OperatingSystem.download(): Path = download(downloadUrl)

    /**
     * Downloads the specified [url].
     *
     * Optionally a [filename] can be provided which is used for logging at places where the [url] would otherwise have been used.
     */
    fun download(url: String, filename: String? = null): Path {
        val handler = customHandlerMapping[url.findScheme()]
        if (handler != null) return handler(URI.create(url))

        val downloadDirectory = downloadDirectory.randomDirectory()
        return downloadDirectory.download(url, filename)
    }
}

typealias Handler = (URI) -> Path
