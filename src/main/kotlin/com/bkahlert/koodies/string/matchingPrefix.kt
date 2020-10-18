package com.bkahlert.koodies.string

fun CharSequence.matchingPrefix(vararg strings: CharSequence): String {
    val expr = strings.joinToString("|") { Regex.escape(it.asString()) }
    return Regex("^($expr)").find(toString())?.value ?: ""
}
