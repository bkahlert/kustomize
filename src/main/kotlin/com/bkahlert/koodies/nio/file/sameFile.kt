package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.string.random
import com.imgcstmzr.util.delete
import java.nio.file.Path

/**
 * Creates a temporary file with the specified [name] always
 * at the same place with as few assumptions about the OS
 * as possible.
 */
fun sameFile(name: String): Path {
    val random = String.random(5)
    val escapedRandom = Regex.escape(random)
    return tempFile(random, random).run {
        delete()
        val replace = conditioned.replace("$escapedRandom.*$escapedRandom".toRegex(), name)
        replace.toPath()
    }
}
