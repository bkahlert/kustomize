package com.bkahlert.koodies.exception

import com.bkahlert.koodies.concurrent.process.LoggedProcess
import java.nio.file.Path

fun Any?.toSingleLineString(): String {
    if (this == null || this == Unit) return ""
    if (this is Path) return "${toUri()}"
    if (this is LoggedProcess) return this.exitValue().toString()
    return this.toString()
}

fun Throwable?.toSingleLineString(): String {
    if (this == null) return ""
    return rootCause.run {
        this::class.simpleName + ": " + message + stackTrace?.firstOrNull()?.let { element -> " at.(${element.fileName}:${element.lineNumber})" }
    }
}

fun Result<*>?.toSingleLineString(): String {
    if (this == null) return ""
    return if (isSuccess) getOrNull().toSingleLineString()
    else exceptionOrNull().toSingleLineString()
}
