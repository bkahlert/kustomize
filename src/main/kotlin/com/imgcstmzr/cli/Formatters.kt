package com.imgcstmzr.cli

import koodies.text.Semantics.formattedAs
import koodies.text.convertCamelCaseToKebabCase
import kotlin.reflect.KProperty

fun CharSequence.asParam(): String = formattedAs.input

fun KProperty<*>.asParam(nameToUse: String = name.convertCamelCaseToKebabCase()): String = nameToUse.asParam()
