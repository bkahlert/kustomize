package com.imgcstmzr.runtime.log

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.result
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.process.Output
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.HasStatus.Companion.status
import com.imgcstmzr.util.stripOffAnsi

class BlockRenderingLogger<in HS : HasStatus>(caption: String) : RenderingLogger<HS> {

    init {
        if (borderedOutput) echo(tc.bold("\n╭─────╴$caption\n│"))
    }

    override fun logLine(output: Output, item: List<HS>, trailingNewline: Boolean): RenderingLogger<HS> {
        if (output.unformatted.isBlank()) return this

        val prefix = if (borderedOutput) "│   " else ""
        output.formattedLines.forEachIndexed { index, line ->
            echo(if (index == 0 && output.type == Output.Type.OUT) {
                val fill = statusPadding(line.stripOffAnsi())
                val status = item.status()
                "$prefix$line$fill$status"
            } else "$prefix$line", trailingNewline)
        }
        return this
    }

    override fun rawLogStart(string: String): RenderingLogger<HS> = rawLog((if (borderedOutput) "│   " else "") + string)

    override fun rawLog(string: String): RenderingLogger<HS> {
        echo(string, trailingNewline = false)
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

        fun render(caption: String, block: (RenderingLogger<HasStatus>) -> Int) {
            val renderer = BlockRenderingLogger<HasStatus>(caption)
            val exitCode = block(renderer)
            renderer.endLogging(caption, exitCode)
        }

        fun render(caption: String): BlockRenderingLogger<HasStatus> = BlockRenderingLogger(caption)
    }
}
