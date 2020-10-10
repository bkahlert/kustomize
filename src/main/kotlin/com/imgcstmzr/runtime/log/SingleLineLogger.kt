package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.terminal.ANSI
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.Output.Companion.format
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.HasStatus.Companion.status
import com.imgcstmzr.util.debug
import kotlin.properties.Delegates.vetoable
import kotlin.reflect.KProperty

abstract class SingleLineLogger<R>(caption: CharSequence) : RenderingLogger<R, HasStatus> {
    init {
        require(caption.isNotBlank()) { "No blank caption allowed." }
    }

    var strings: List<String>? by vetoable(listOf("$caption:"),
        onChange = { property: KProperty<*>, oldValue: List<String>?, newValue: List<String>? -> oldValue != null })

    override fun logLine(
        output: Output,
        items: List<HasStatus>,
    ): RenderingLogger<R, HasStatus> {
        strings = strings?.plus(output.formattedLines.joinToString(", "))
        if (items.isNotEmpty()) strings = strings?.plus(items.status().lines().joinToString(", ", "(", ")"))
        return this
    }

    fun logLast(result: Result<R>): R {
        return kotlin.runCatching {
            val message = ANSI.EscapeSequences.termColors.bold(
                if (result.isSuccess) "✔" + if (result.getOrNull() === Unit) "" else " with ${result.getOrNull().debug}"
                else ANSI.EscapeSequences.termColors.red("Failure: ${result.exceptionOrNull()}")
            )
            val string: String = strings?.plus(message)?.joinToString(" ") ?: ""
            log(string, true)
            result.getOrThrow()
        }.onFailure { log(it.format(), false) }.getOrThrow()
    }
}
