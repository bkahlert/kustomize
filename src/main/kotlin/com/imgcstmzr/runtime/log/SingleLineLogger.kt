package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ansi.Style.Companion.bold
import com.bkahlert.koodies.terminal.ansi.Style.Companion.italic
import com.imgcstmzr.process.Output
import com.imgcstmzr.process.Output.Companion.format
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.HasStatus.Companion.status
import com.imgcstmzr.util.debug
import kotlin.properties.Delegates.vetoable
import kotlin.reflect.KProperty

abstract class SingleLineLogger<R>(caption: CharSequence) : RenderingLogger<R> {
    init {
        require(caption.isNotBlank()) { "No blank caption allowed." }
    }

    var strings: List<String>? by vetoable(listOf("$caption:"),
        onChange = { property: KProperty<*>, oldValue: List<String>?, newValue: List<String>? -> oldValue != null })

    override fun logLineLambda(items: List<HasStatus>, block: () -> Output): RenderingLogger<R> {
        strings = strings?.plus(block().formattedLines.joinToString(", "))
        if (items.isNotEmpty()) strings = strings?.plus(items.status().lines().joinToString(", ", "(", ")"))
        return this
    }

    fun logLast(result: Result<R>): R {
        return kotlin.runCatching {
            val message = (
                if (result.isSuccess) "✔" + if (result.getOrNull() === Unit) "" else " returned".italic() + " ${result.getOrNull().debug}"
                else ANSI.EscapeSequences.termColors.red("Failure: ${result.exceptionOrNull()}")
                ).bold()
            val string: String = strings?.plus(message)?.joinToString(" ") ?: ""
            log(string, true)
            result.getOrThrow()
        }.onFailure { log(it.format(), false) }.getOrThrow()
    }
}
