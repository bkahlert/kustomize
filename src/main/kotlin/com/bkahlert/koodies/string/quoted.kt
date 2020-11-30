package com.bkahlert.koodies.string

inline val CharSequence?.quoted: String get() = (this ?: "␀").wrap("\"")
inline val CharSequence?.singleQuoted: String get() = (this ?: "␀").wrap("'")
