package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.unit.Size.Companion.size
import com.imgcstmzr.util.isDirectory
import com.imgcstmzr.util.isFile
import java.math.BigDecimal
import java.nio.file.Path

/**
 * Depending on the file type returns if
 * - this file is not empty, that is, has positive length
 * - this directory is not empty, that is, has entries
 *
 * @throws IllegalArgumentException if this is neither a file nor a directory
 */
val Path.isNotEmpty: Boolean
    get() {
        requireExists()
        return when {
            isFile -> size.bytes > BigDecimal.ZERO
            isDirectory -> list().any()
            else -> throw IllegalArgumentException("$this must either be a file or a directory.")
        }
    }
