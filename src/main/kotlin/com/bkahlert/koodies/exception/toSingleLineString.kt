package com.bkahlert.koodies.exception

import com.bkahlert.koodies.concurrent.process.Process
import com.bkahlert.koodies.string.LineSeparators.lines
import java.nio.file.Path

fun Any?.toSingleLineString(): String {
    if (this == null || this == Unit) return ""
    if (this is Path) return "${toUri()}"
    if (this is Process) return also { waitFor() }.exitValue.toString()
    if (this is java.lang.Process) return also { waitFor() }.exitValue().toString()
    return this.toString()
}

fun Throwable?.toSingleLineString(): String {
    if (this == null) return ""
    val messagePart = message?.let { ": " + it.lines()?.get(0) } ?: ""
    return rootCause.run {
        this::class.simpleName + messagePart + stackTrace?.firstOrNull()
            ?.let { element -> " at.(${element.fileName}:${element.lineNumber})" }
    }
}

fun Result<*>?.toSingleLineString(): String {
    if (this == null) return ""
    return if (isSuccess) getOrNull().toSingleLineString()
    else exceptionOrNull().toSingleLineString()
}
