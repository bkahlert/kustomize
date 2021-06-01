package com.imgcstmzr.util

import com.imgcstmzr.Locations
import com.imgcstmzr.os.OperatingSystem
import koodies.docker.download
import koodies.io.path.randomDirectory
import koodies.logging.FixedWidthRenderingLogger
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
class Downloader(private val downloadDirectory: Path = Locations.Temp, vararg customHandlers: Pair<String, Handler>) {
    private val customHandlerMapping = customHandlers.toMap()

    private fun String.findScheme() = kotlin.runCatching { URI.create(this).scheme }.getOrElse { IllegalArgumentException("Invalid URI", it) }

    /**
     * Downloads an image copy containing the [OperatingSystem] and returns the [Path] where the
     * copy can be found after successful download.
     */
    fun OperatingSystem.download(logger: FixedWidthRenderingLogger): Path = download(downloadUrl, logger = logger, filename = fullName)

    /**
     * Downloads the specified [url], [retries] the specified amount of times.
     *
     * Optionally a [filename] can be provided which is used for logging at places where the [url] would otherwise have been used.
     */
    fun download(url: String, logger: FixedWidthRenderingLogger, retries: Int = 5, filename: String? = null): Path {
        val handler = customHandlerMapping[url.findScheme()]
        if (handler != null) return handler(URI.create(url), logger)

        val downloadDirectory = downloadDirectory.randomDirectory()
        return logger.compactLogging("Downloading ${filename ?: url} …") {
            downloadDirectory.download(url, logger = null)
        }
    }
}

typealias Handler = (URI, FixedWidthRenderingLogger) -> Path
