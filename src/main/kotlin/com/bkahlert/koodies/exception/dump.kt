package com.bkahlert.koodies.exception

import com.bkahlert.koodies.concurrent.cleanUpOldTempFiles
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.string.LineSeparators.LF
import com.bkahlert.koodies.string.joinLinesToString
import com.bkahlert.koodies.string.withSuffix
import java.nio.file.Path
import kotlin.time.days

private object Dump {
    const val dumpPrefix = "koodies.dump."
    const val dumpSuffix = ".log"

    init {
        cleanUpOldTempFiles(dumpPrefix, dumpSuffix, 5.days)
    }
}

@Suppress("unused")
private val dumpInitWorkaround = "$Dump"

/**
 * Dumps whatever is returned by [data] to the specified [path] and
 * returns a description of the dump.
 *
 * If an error occurs in this process—so to as a last resort—the returned description
 * includes the complete dump itself.
 */
fun dump(errorMessage: String?, path: Path = tempFile(Dump.dumpPrefix, Dump.dumpSuffix), data: () -> String): String = runCatching {
    var dumped: String? = null
    val dumps = persistDump(path) { data().also { dumped = it } }

    val dumpedLines = (dumped ?: error("Dump seems empty")).lines()
    val recentLineCount = dumpedLines.size.coerceAtMost(10)

    (errorMessage?.withSuffix(LF)?.capitalize() ?: "") +
        "➜ A dump has been written to:$LF" +
        dumps.entries.joinLinesToString(postfix = LF) { "  - ${it.value.toUri()} (${it.key})" } +
        "➜ The last $recentLineCount lines are:$LF" +
        dumpedLines.takeLast(recentLineCount).map { "  $it" }.joinLinesToString(postfix = LF)
}.recover { ex: Throwable ->
    (errorMessage?.withSuffix(LF)?.capitalize() ?: "") +
        "In the attempt to persist the corresponding dump the following error occurred:$LF" +
        "${ex.toSingleLineString()}$LF" +
        LF +
        "➜ The not successfully persisted dump is as follows:$LF" +
        data()
}.getOrThrow()

/**
 * Dumps whatever is returned by [data] to the specified [path] and
 * returns a description of the dump.
 *
 * If an error occurs in this process—so to as as a last resort—the returned description
 * includes the complete dump itself.
 */
@Suppress("unused")
fun dump(errorMessage: String?, path: Path = tempFile(Dump.dumpPrefix, Dump.dumpSuffix), data: String): String = dump(errorMessage, path) { data }
