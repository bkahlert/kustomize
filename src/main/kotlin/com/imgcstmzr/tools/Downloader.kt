package com.imgcstmzr.tools

import com.bkahlert.koodies.concurrent.process.executeShellScript
import com.bkahlert.koodies.nio.file.list
import com.bkahlert.koodies.nio.file.tempDir
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.singleLineLogging
import com.imgcstmzr.util.cleanUp
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
class Downloader(vararg customHandlers: Pair<String, Handler>) {
    private val customHandlerMapping = customHandlers.toMap()

    private fun String.findScheme() = kotlin.runCatching { URI.create(this).scheme }.getOrElse { IllegalArgumentException("Invalid URI", it) }

    /**
     * Downloads an image copy containing the [OperatingSystem] and returns the [Path] where the
     * copy can be found after successful download.
     */
    fun OperatingSystem.download(logger: BlockRenderingLogger): Path = download(downloadUrl, logger = logger, filename = fullName)

    /**
     * Downloads the specified [url], [retries] the specified amount of times.
     *
     * Optionally a [filename] can be provided which is used for logging at places where the [url] would otherwise have been used.
     */
    fun download(url: String, logger: BlockRenderingLogger, retries: Int = 5, filename: String? = null): Path =
        customHandlerMapping[url.findScheme()]?.invoke(URI.create(url), logger) ?: tempDir("imgcstmzr").let { temp ->
            logger.singleLineLogging("Downloading ${filename ?: url} to $temp") {

                // TODO parse progress and feedback
                executeShellScript(workingDirectory = temp) {
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
                }.waitFor()

                temp.list().singleOrNull()?.cleanUp() ?: throw IllegalStateException("Failed to download $url.")
            }
        }
}

typealias Handler = (URI, BlockRenderingLogger) -> Path
