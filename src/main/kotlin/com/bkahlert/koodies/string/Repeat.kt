package com.bkahlert.koodies.string


fun Char.repeat(count: Int): String = String(CharArray(count) { this })

fun <T : CharSequence> T.repeat(count: Int): String = StringBuilder().also { repeat(count) { _ -> it.append(this@repeat) } }.toString()
