package com.bkahlert.koodies.string

import kotlin.streams.asSequence

/**
 * Returns a sequence containing the [CodePoint] instances this string consists of.
 */
fun CharSequence.codePointSequence(): Sequence<CodePoint> =
    codePoints().mapToObj { CodePoint(it) }.asSequence()

/**
 * Returns a sequence containing the [CodePoint] instances this string consists of.
 */
fun String.codePointSequence(): Sequence<CodePoint> =
    (this as CharSequence).codePointSequence()
