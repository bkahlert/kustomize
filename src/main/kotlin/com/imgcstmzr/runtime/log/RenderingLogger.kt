package com.imgcstmzr.runtime.log

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.OutputType
import com.imgcstmzr.runtime.Program
import com.imgcstmzr.util.splitLineBreaks

interface RenderingLogger<in P : Program<*>> {
    fun log(output: Output): RenderingLogger<P> = this

    fun log(output: Output, programs: List<P> = emptyList()): RenderingLogger<P> {
        log(output)
        return this
    }

    companion object {
        fun prepareLines(output: Output): List<String> =
            output.raw.splitLineBreaks().mapIndexed { index, line ->
                when (output.type) {
                    OutputType.OUT -> line
                    OutputType.ERR -> (tc.red + tc.italic)(line)
                    OutputType.META -> (tc.gray + tc.italic)(line)
                    else -> (tc.magenta + tc.bold)(line)
                }
            }

        val DEFAULT = object : RenderingLogger<Program<*>> {
            override fun log(output: Output): RenderingLogger<Program<*>> {
                echo(prepareLines(output).joinToString(System.lineSeparator()))
                return this
            }
        }
    }
}
