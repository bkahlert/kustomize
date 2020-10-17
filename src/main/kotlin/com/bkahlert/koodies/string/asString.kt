package com.bkahlert.koodies.string

fun CharSequence.asString(): String = this as? String ?: toString()
