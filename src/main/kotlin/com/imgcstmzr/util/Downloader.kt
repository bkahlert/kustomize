package com.imgcstmzr.util

import com.imgcstmzr.runtime.OperatingSystem
import koodies.concurrent.script
import koodies.io.path.Locations
import koodies.io.path.asString
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.randomDirectory
import koodies.io.path.renameTo
import koodies.logging.RenderingLogger
import koodies.logging.compactLogging
import koodies.runtime.deleteOnExit
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
    fun OperatingSystem.download(logger: RenderingLogger): Path = download(downloadUrl, logger = logger, filename = fullName)

    /**
     * Downloads the specified [url], [retries] the specified amount of times.
     *
     * Optionally a [filename] can be provided which is used for logging at places where the [url] would otherwise have been used.
     */
    fun download(url: String, logger: RenderingLogger, retries: Int = 5, filename: String? = null): Path {
        val handler = customHandlerMapping[url.findScheme()]
        if (handler != null) return handler(URI.create(url), logger)

        val downloadDirectory = downloadDirectory.randomDirectory()
        return logger.compactLogging("Downloading ${filename ?: url} …") {
            // TODO parse progress and feedback
            downloadDirectory.script {
                !"for i in ${(1..retries).joinToString(" ")} ; do"
                command(
                    "2>&1",
                    "curl",
                    "--location",
                    "--remote-name",
                    "--remote-header-name",
                    "--compressed",
                    "--retry 5",
                    "--retry-delay 5",
                    "--retry-max-time 300",
                    url,
                )
                !"done"
                deleteSelf()
            }.waitForTermination()

            downloadDirectory.listDirectoryEntriesRecursively().singleOrNull()?.cleanFileName()?.deleteOnExit()
                ?: throw IllegalStateException("Failed to download $url.")
        }
    }

    private fun Path.cleanFileName(): Path {
        val cleanedFileName = listOf("?", "#").fold(fileName.asString()) { acc, symbol -> acc.substringBefore(symbol) }
        return if (fileName.asString() != cleanedFileName) renameTo(cleanedFileName) else this
    }
}

typealias Handler = (URI, RenderingLogger) -> Path
