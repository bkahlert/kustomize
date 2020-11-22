package com.bkahlert.koodies.nio.file

import java.net.URLConnection
import java.nio.file.Path

/**
 * Contains the likely MIME type guessed from the file name.
 */
val Path.guessedMimeType: String?
    get() = kotlin.runCatching { URLConnection.guessContentTypeFromName(fileName.serialized) }.getOrNull()
