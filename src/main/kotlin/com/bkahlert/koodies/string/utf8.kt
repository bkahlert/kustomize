package com.bkahlert.koodies.string

val CharSequence.utf8: ByteArray get() = toByteArray(Charsets.UTF_8)
