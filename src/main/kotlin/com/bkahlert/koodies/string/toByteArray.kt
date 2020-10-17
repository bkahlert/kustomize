package com.bkahlert.koodies.string

import java.nio.charset.Charset
import kotlin.text.toByteArray as toKotlinByteArray

fun CharSequence.toByteArray(charset: Charset = Charsets.UTF_8): ByteArray = asString().toKotlinByteArray(charset)


