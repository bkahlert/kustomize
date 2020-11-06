package com.bkahlert.koodies.regex

import com.bkahlert.koodies.terminal.ansi.AnsiCode

object RegularExpressions {
    val atLeastOneWhitespaceRegex: Regex = Regex("\\s+")
    val urlRegex: Regex = Regex("(?<schema>https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
    val uriRegex: Regex = Regex("\\w+:(?:/?/?)[^\\s]+")
    val ansiCodeRegex: Regex = AnsiCode.ansiCodeRegex
}
