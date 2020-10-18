package com.imgcstmzr.runtime.log

import com.imgcstmzr.process.Output
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.HasStatus.Companion.status
import kotlin.properties.Delegates.vetoable
import kotlin.reflect.KProperty

abstract class SingleLineLogger<R>(caption: CharSequence) : RenderingLogger<R> {
    init {
        require(caption.isNotBlank()) { "No blank caption allowed." }
    }

    var strings: List<String>? by vetoable(listOf("$caption:"),
        onChange = { property: KProperty<*>, oldValue: List<String>?, newValue: List<String>? -> oldValue != null })

    override fun logLambda(trailingNewline: Boolean, block: () -> String) {
        val element = block()
        strings = strings?.plus(element)
    }

    override fun logLineLambda(items: List<HasStatus>, block: () -> Output): RenderingLogger<R> {
        strings = strings?.plus(block().formattedLines.joinToString(", "))
        if (items.isNotEmpty()) strings = strings?.plus(items.status().lines().joinToString(", ", "(", ")"))
        return this
    }

    override fun logLastLambda(block: () -> Result<R>): R {
        val returnValue = super.logLastLambda(block)
        render { strings?.joinToString(" ") ?: "" }
        return returnValue
    }

    abstract fun render(block: () -> String)
}
