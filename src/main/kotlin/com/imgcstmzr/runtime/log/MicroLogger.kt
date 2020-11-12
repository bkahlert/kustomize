package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.string.Grapheme
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.HasStatus.Companion.status
import kotlin.properties.Delegates.vetoable
import kotlin.reflect.KProperty

abstract class MicroLogger<R>(private val symbol: Grapheme? = null) : RenderingLogger<R> {

    var strings: List<String>? by vetoable(listOf(),
        onChange = { _: KProperty<*>, oldValue: List<String>?, _: List<String>? -> oldValue != null })

    abstract fun render(block: () -> CharSequence)

    override fun render(trailingNewline: Boolean, block: () -> CharSequence) {
        strings = strings?.plus("${block()}")
    }

    override fun logStatus(items: List<HasStatus>, block: () -> IO) {
        strings = strings?.plus(block().formatted.lines().joinToString(", "))
        if (items.isNotEmpty()) strings =
            strings?.plus(items.status().lines().size.let { "($it)" })
    }

    override fun logResult(block: () -> Result<R>): R {
        val returnValue = super.logResult(block)
        render { strings?.joinToString(prefix = "(" + (symbol?.let { "$it " } ?: ""), separator = " ˃ ", postfix = ")") ?: "" }
        return returnValue
    }
}
