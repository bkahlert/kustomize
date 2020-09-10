package com.imgcstmzr.runtime.log

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.process.Output
import com.imgcstmzr.runtime.HasStatus

interface RenderingLogger<in HS : HasStatus> {

    fun logLine(output: Output, items: List<HS>, trailingNewline: Boolean): RenderingLogger<HS>

    fun logLine(output: Output): RenderingLogger<HS> = logLine(output, emptyList(), true)

    fun logLine(output: Output, trailingNewline: Boolean): RenderingLogger<HS> = logLine(output, emptyList(), trailingNewline)

    fun logLine(output: Output, items: List<HS>): RenderingLogger<HS> = logLine(output, items, true)

    fun rawLogStart(string: String): RenderingLogger<HS>

    fun rawLog(string: String): RenderingLogger<HS>

    fun rawLogEnd(string: String = ""): RenderingLogger<HS> = rawLog(string + System.lineSeparator())

    companion object {

        val DEFAULT: RenderingLogger<HasStatus> = object : RenderingLogger<HasStatus> {
            override fun logLine(output: Output, items: List<HasStatus>, trailingNewline: Boolean): RenderingLogger<HasStatus> {
                echo(output.formattedLines.joinToString(System.lineSeparator()), trailingNewline)
                return this
            }

            override fun rawLogStart(string: String): RenderingLogger<HasStatus> = rawLog(string)

            override fun rawLog(string: String): RenderingLogger<HasStatus> {
                echo(string, trailingNewline = false)
                return this
            }
        }
    }
}
