package com.imgcstmzr.cli

import koodies.text.ANSI.Formatter
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.Semantics.formattedAs
import koodies.text.convertCamelCaseToKebabCase
import koodies.tracing.rendering.ColumnsLayout
import koodies.tracing.rendering.ColumnsLayout.Companion.columns
import koodies.tracing.rendering.RenderingAttributes
import kotlin.reflect.KProperty

fun CharSequence.asParam(): String = formattedAs.input

fun KProperty<*>.asParam(nameToUse: String = name.convertCamelCaseToKebabCase()): String = nameToUse.asParam()

val PATCH_DECORATION_FORMATTER: Formatter<CharSequence> = Formatter { it.ansi.green.done }

object Layouts {

    /**
     * Two columns layout consisting of:
     * - one column that renders [RenderingAttributes.DESCRIPTION]`/160`
     */
    val DESCRIPTION: ColumnsLayout = ColumnsLayout(
        RenderingAttributes.DESCRIPTION columns 160,
        maxColumns = 160,
    )

    /**
     * Two columns layout consisting of:
     * - one column that renders [RenderingAttributes.DESCRIPTION]`/120`
     * - one column that renders [RenderingAttributes.EXTRA]`/40`.
     */
    val DESCRIPTION_AND_EXTRA: ColumnsLayout = ColumnsLayout(
        RenderingAttributes.DESCRIPTION columns 120,
        RenderingAttributes.EXTRA columns 40,
        maxColumns = 160,
    )
}
