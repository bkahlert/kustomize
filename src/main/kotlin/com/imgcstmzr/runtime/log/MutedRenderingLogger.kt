package com.imgcstmzr.runtime.log

/**
 * A logger that can be used if no logging is needed.
 */
open class MutedRenderingLogger : RenderingLogger {

    override fun render(trailingNewline: Boolean, block: () -> CharSequence) {}

    override fun toString(): String = "MutedBlockRenderingLogger"
}
