package com.bkahlert.koodies.string

fun CharSequence?.wrap(value: CharSequence): String = "$value${this ?: "␀"}$value"
fun CharSequence?.wrap(left: CharSequence, right: CharSequence): String = "$left${this ?: "␀"}$right"
