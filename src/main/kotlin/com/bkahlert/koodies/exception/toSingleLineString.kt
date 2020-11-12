package com.bkahlert.koodies.exception

import com.bkahlert.koodies.concurrent.process.CompletedProcess
import java.nio.file.Path

fun Any?.toSingleLineString(): String {
    if (this == null || this == Unit) return ""
    if (this is Path) return "${toUri()}"
    if (this is CompletedProcess) return this.exitCode.toString()
    return this.toString()
}

fun Throwable?.toSingleLineString(): String {
    if (this == null) return ""
    return rootCause.let {
        it::class.simpleName + ": " + it.message + it.stackTrace?.firstOrNull()?.let { element -> " at.(${element.fileName}:${element.lineNumber})" }
    }
}

fun Result<*>?.toSingleLineString(): String {
    if (this == null) return ""
    return if (isSuccess) getOrNull().toSingleLineString()
    else exceptionOrNull().toSingleLineString()
}
