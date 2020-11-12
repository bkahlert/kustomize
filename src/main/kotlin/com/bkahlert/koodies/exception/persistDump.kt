package com.bkahlert.koodies.exception

import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.imgcstmzr.util.withExtension
import com.imgcstmzr.util.writeText
import java.io.IOException
import java.nio.file.Path

/**
 * Dumps whatever is returned by [data] to the specified [path].
 *
 * This method returns a map of file format to [Path] mappings.
 */
fun persistDump(path: Path, data: () -> String): Map<String, Path> = runCatching {
    data().run {
        mapOf("unchanged" to path.withExtension("log").writeText(this),
            "ANSI escape/control sequences removed" to path.withExtension("no-ansi.log").writeText(removeEscapeSequences()))
    }
}.getOrElse {
    if (it is IOException) throw it
    throw IOException(it)
}
