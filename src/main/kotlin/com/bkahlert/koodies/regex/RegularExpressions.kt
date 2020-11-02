package com.bkahlert.koodies.regex

object RegularExpressions {
    val urlRegex: Regex = Regex("(?<schema>https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]")
    val uriRegex: Regex = Regex("\\w+:(?:/?/?)[^\\s]+")
}
