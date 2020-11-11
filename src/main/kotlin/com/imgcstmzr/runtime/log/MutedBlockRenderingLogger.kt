package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.imgcstmzr.runtime.HasStatus

/**
 * A logger that can be used if no logging is needed.
 */
open class MutedBlockRenderingLogger<R>(
    caption: CharSequence = "",
    borderedOutput: Boolean = false,
    log: (CharSequence) -> Any = { },
) : BlockRenderingLogger<R>(
    caption = caption,
    borderedOutput = borderedOutput,
    log = log) {

    override fun render(trailingNewline: Boolean, block: () -> CharSequence) {}

    override fun logStatus(items: List<HasStatus>, block: () -> IO) {}

    override fun logResult(block: () -> Result<R>): R = block().getOrThrow()

    override fun toString(): String = "MutedBlockRenderingLogger()"
}
