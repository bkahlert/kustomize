package com.bkahlert.koodies.terminal.ansi

import com.bkahlert.koodies.string.asString
import com.bkahlert.koodies.terminal.ANSI

object AnsiFormats {
    fun CharSequence.bold() = ANSI.termColors.bold(asString())
    fun CharSequence.dim() = ANSI.termColors.dim(asString())
    fun CharSequence.italic() = ANSI.termColors.italic(asString())
    fun CharSequence.underline() = ANSI.termColors.underline(asString())
    fun CharSequence.inverse() = ANSI.termColors.inverse(asString())
    fun CharSequence.hidden() = ANSI.termColors.hidden(asString())
    fun CharSequence.strikethrough() = ANSI.termColors.strikethrough(asString())
}
