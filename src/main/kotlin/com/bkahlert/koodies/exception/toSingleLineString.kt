package com.bkahlert.koodies.exception

import com.imgcstmzr.util.debug

fun Any?.toSingleLineString(): String? {
    if (this == null || this == Unit) return null
    return this.debug
}

fun Throwable?.toSingleLineString(): String? {
    if (this == null) return null
    return this::class.qualifiedName + ": " + message + " @ " + stackTrace?.first()
}

fun Result<*>?.toSingleLineString(): String? {
    if (this == null) return ""
    return if (isSuccess) getOrNull().toSingleLineString()
    else exceptionOrNull()?.toSingleLineString()
}
