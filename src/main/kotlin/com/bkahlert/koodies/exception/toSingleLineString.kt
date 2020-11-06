package com.bkahlert.koodies.exception

import com.imgcstmzr.util.debug

fun Any?.toSingleLineString(): String {
    if (this == null || this == Unit) return ""
    return this.debug
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
