package com.bkahlert.koodies.string

import com.bkahlert.koodies.collections.zipWithDefault
import com.bkahlert.koodies.string.LineSeparators.lineSequence
import com.bkahlert.koodies.terminal.ansi.AnsiString
import com.bkahlert.koodies.terminal.ansi.AnsiString.Companion.asAnsiString

/**
 * Returns a string that consists of two columns.
 * 1) This char sequence as the first column.
 * 2) The other char sequence as the second column.
 *
 * **Example**
 * ```
 * Line 1
 * Line 1.1
 * Line 2
 * ```
 * and
 * ```
 * Line a
 * Line a.b
 * Line c
 * Line d
 * ```
 * will result in
 * ```
 * Line 1       Line 1
 * Line 1.1     Line a.b
 * Line 2       Line c
 *              Line d
 * ```
 */
fun AnsiString.addColumn(column: AnsiString, columnWidth: Int = maxLineLength(), paddingCharacter: Char = ' ', paddingWidth: Int = 5): AnsiString =
    lineSequence()
        .zipWithDefault(column.lineSequence(), "" to "") { leftLine: String, rightLine: String ->
            val leftColumn = leftLine.asAnsiString().padEnd(columnWidth + paddingWidth, padChar = paddingCharacter)
            "$leftColumn$rightLine"
        }
        .joinLinesToString()
        .asAnsiString()

/**
 * Returns a string that consists of two columns.
 * 1) This char sequence as the first column.
 * 2) The other char sequence as the second column.
 *
 * **Example**
 * ```
 * Line 1
 * Line 1.1
 * Line 2
 * ```
 * and
 * ```
 * Line a
 * Line a.b
 * Line c
 * Line d
 * ```
 * will result in
 * ```
 * Line 1       Line 1
 * Line 1.1     Line a.b
 * Line 2       Line c
 *              Line d
 * ```
 */
fun CharSequence.addColumn(
    column: CharSequence,
    columnWidth: Int = asAnsiString().maxLineLength(),
    paddingCharacter: Char = ' ',
    paddingWidth: Int = 5,
): String =
    asAnsiString().addColumn(
        column = column.asAnsiString(),
        columnWidth = columnWidth,
        paddingCharacter = paddingCharacter,
        paddingWidth = paddingWidth,
    ).toString()
