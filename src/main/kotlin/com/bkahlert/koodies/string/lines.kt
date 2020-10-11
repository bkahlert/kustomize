package com.bkahlert.koodies.string

fun <T : Any> Sequence<T>.lines(
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null,
): String = joinToString(
    separator = LineSeparators.LF,
    prefix = prefix,
    postfix = postfix,
    limit = limit,
    truncated = truncated,
    transform = transform
)

fun <T : Any> Iterable<T>.lines(
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    limit: Int = -1,
    truncated: CharSequence = "...",
    transform: ((T) -> CharSequence)? = null,
): String = joinToString(
    separator = LineSeparators.LF,
    prefix = prefix,
    postfix = postfix,
    limit = limit,
    truncated = truncated,
    transform = transform
)
