package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.HasStatus.Companion.status
import kotlin.properties.Delegates.vetoable
import kotlin.reflect.KProperty

abstract class SingleLineLogger(caption: CharSequence) : RenderingLogger {
    init {
        require(caption.isNotBlank()) { "No blank caption allowed." }
    }

    var strings: List<String>? by vetoable(listOf("$caption:"),
        onChange = { _: KProperty<*>, oldValue: List<String>?, _: List<String>? -> oldValue != null })

    abstract fun render(block: () -> CharSequence)

    override fun render(trailingNewline: Boolean, block: () -> CharSequence) {
        strings = strings?.plus("${block()}")
    }

    override fun logStatus(items: List<HasStatus>, block: () -> IO) {
        strings = strings?.plus(block().formatted.lines().joinToString(", "))
        if (items.isNotEmpty()) strings =
            strings?.plus(items.status().lines().joinToString(", ", "(", ")"))
    }

    override fun <R> logResult(block: () -> Result<R>): R {
        val returnValue = super.logResult(block)
        render { strings?.joinToString(" ") ?: "" }
        return returnValue
    }
}
