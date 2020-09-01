package com.imgcstmzr.runtime

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.result
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.OutputType
import com.imgcstmzr.runtime.Program.Companion.status
import com.imgcstmzr.util.stripOffAnsi

class BlockRenderingLogger<in P : Program<*>>(caption: String) : RenderingLogger<P> {

    init {
        if (borderedOutput) echo(tc.bold("\n╭─────╴$caption\n│"))
    }

    override fun log(output: Output, programs: List<P>): RenderingLogger<P> {
        if (output.unformatted.isBlank()) return this

        val prefix = if (borderedOutput) "│   " else ""
        RenderingLogger.prepareLines(output).forEachIndexed { index, line ->
            echo(if (index == 0 && output.type == OutputType.OUT) {
                val fill = statusPadding(line.stripOffAnsi())
                val status = programs.status()
                "$prefix$line$fill$status"
            } else "$prefix$line")
        }
        return this
    }

    fun endLogging(caption: String, result: Int) {
        if (borderedOutput) echo(tc.bold("│\n╰─────╴$caption : ${result(result)}\n"))
    }

    companion object {
        private const val borderedOutput: Boolean = true
        private const val statusInformationColumn = 100
        private const val statusInformationMinimalPadding = 5

        private val statusPadding = { text: String ->
            " ".repeat((statusInformationColumn - text.stripOffAnsi().length).coerceAtLeast(statusInformationMinimalPadding))
        }

        fun <PROGRAM : Program<*>> render(caption: String, block: (RenderingLogger<PROGRAM>) -> Int) {
            val renderer = BlockRenderingLogger<PROGRAM>(caption)
            val exitCode = block(renderer)
            renderer.endLogging(caption, exitCode)
        }

        fun <PROGRAM : Program<*>> render(caption: String): BlockRenderingLogger<PROGRAM> = BlockRenderingLogger<PROGRAM>(caption)
    }
}
