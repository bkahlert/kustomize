package com.bkahlert.koodies.terminal.ansi

import com.github.ajalt.mordant.AnsiColorCode
import com.github.ajalt.mordant.TermColors

typealias ColorProvider = (TermColors.() -> AnsiColorCode)
