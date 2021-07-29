package com.bkahlert.kustomize.cli

import com.bkahlert.kustomize.os.OperatingSystems
import com.github.ajalt.clikt.parameters.options.NullableOption
import com.github.ajalt.clikt.parameters.options.RawOption
import com.github.ajalt.clikt.parameters.types.choice

object Options {
    fun RawOption.os(
        choices: Map<String, OperatingSystems> = OperatingSystems.values().associateBy { it.name },
    ): NullableOption<OperatingSystems, OperatingSystems> = choice(choices = choices, ignoreCase = true)
}
