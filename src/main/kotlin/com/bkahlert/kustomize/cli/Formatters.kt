package com.bkahlert.kustomize.cli

import com.bkahlert.kommons.text.ANSI.Formatter
import com.bkahlert.kommons.text.ANSI.Text.Companion.ansi
import com.bkahlert.kommons.text.Semantics.formattedAs
import com.bkahlert.kommons.text.convertCamelCaseToKebabCase
import com.bkahlert.kommons.tracing.rendering.ColumnsLayout
import com.bkahlert.kommons.tracing.rendering.ColumnsLayout.Companion.columns
import com.bkahlert.kommons.tracing.rendering.RenderingAttributes
import kotlin.reflect.KProperty

fun CharSequence.asParam(): String = formattedAs.input

fun KProperty<*>.asParam(nameToUse: String = name.convertCamelCaseToKebabCase()): String = nameToUse.asParam()

val PATCH_NAME_FORMATTER: Formatter<CharSequence> = Formatter { it.ansi.cyan.done }
val PATCH_DECORATION_FORMATTER: Formatter<CharSequence> = Formatter { it.ansi.cyan.done }
val PATCH_INNER_DECORATION_FORMATTER: Formatter<CharSequence> = Formatter { it.ansi.green.done }

object Layouts {

    /**
     * One column layout that renders [RenderingAttributes.DESCRIPTION]`/160`
     */
    val DESCRIPTION: ColumnsLayout = ColumnsLayout(
        RenderingAttributes.DESCRIPTION columns 160,
        maxColumns = 160,
    )
}
