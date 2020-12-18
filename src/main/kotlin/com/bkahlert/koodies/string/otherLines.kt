package com.bkahlert.koodies.string

import com.bkahlert.koodies.collections.tail
import com.bkahlert.koodies.string.LineSeparators.lines

/**
 * If this character sequence is made up of multiple lines of text,
 * this property contains all but the first.
 *
 * @see CharSequence.firstLine
 */
val CharSequence.otherLines: List<String>
    get() = lines().tail
