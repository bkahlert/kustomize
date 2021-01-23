package com.imgcstmzr.cli

import koodies.terminal.AnsiColors.cyan
import koodies.text.convertCamelCaseToKebabCase
import kotlin.reflect.KProperty

fun CharSequence.asParam(): String = cyan()

fun KProperty<*>.asParam(nameToUse: String = name.convertCamelCaseToKebabCase()): String = nameToUse.asParam()
