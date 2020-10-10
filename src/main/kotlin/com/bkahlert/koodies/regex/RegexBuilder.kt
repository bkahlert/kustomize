package com.bkahlert.koodies.regex

import com.bkahlert.koodies.string.CharRanges

object RegexBuilder {
    val alphanumericCharacters = (CharRanges.`0-9` + CharRanges.`A-Z` + CharRanges.`a-z`).toCharArray()
    val alphanumericCapitalCharacters = (CharRanges.`0-9` + CharRanges.`A-Z`).toCharArray()
    
    fun characterClass(vararg characters: String) = "[${Regex.escape(characters.joinToString(""))}]"
    fun negatedCharacterClass(vararg characters: String) = "[^${Regex.escape(characters.joinToString(""))}]"
    fun ranges(vararg ranges: Pair<String, String>) = "[" + ranges.joinToString("") { (from, to) -> Regex.escape(from) + "-" + Regex.escape(to) } + "]"
    fun range(from: String, to: String) = ranges(from to to)
}
