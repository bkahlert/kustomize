package com.imgcstmzr.cli

import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.types.choice
import com.imgcstmzr.runtime.OperatingSystems

object Options {
    fun RawOption.os(
        choices: Map<String, OperatingSystems> = OperatingSystems.supported.associateBy { it::class.simpleName!! },
    ): NullableOption<OperatingSystems, OperatingSystems> = choice(choices = choices, ignoreCase = true)
}
