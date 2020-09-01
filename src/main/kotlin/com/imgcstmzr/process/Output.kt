package com.imgcstmzr.process

import com.imgcstmzr.util.stripOffAnsi

/**
 * Instances are [raw] output with a certain [OutputType].
 */
class Output private constructor(val raw: String, val type: OutputType) : CharSequence by raw {
    val unformatted: String = raw.stripOffAnsi()
    val isBlank: Boolean = unformatted.isBlank()

    companion object {
        fun CharSequence.ofType(outputType: OutputType): Output = Output(this.toString(), outputType)
    }

    override fun toString(): String = "${type.symbol}⟨${raw}⟩"
}
