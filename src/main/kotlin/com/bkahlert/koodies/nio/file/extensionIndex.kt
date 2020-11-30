package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Contains the position of the period `.` separating the extension from this
 * [serialized] path.
 */
val Path.extensionIndex
    get() = serialized.lastIndexOf(".").takeIf { fileName.serialized.contains(".") } ?: -1
