package com.bkahlert.koodies.string

val CharSequence.trailingWhitespaces: String get() = Regex("[${Regex.fromLiteral(Unicode.whitespaces.joinToString(""))}]$").find(toString())?.value ?: ""
