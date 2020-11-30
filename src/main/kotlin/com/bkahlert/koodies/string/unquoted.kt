package com.bkahlert.koodies.string

inline val CharSequence.unquoted: String get() = "${unwrap("\"", "\'")}"
