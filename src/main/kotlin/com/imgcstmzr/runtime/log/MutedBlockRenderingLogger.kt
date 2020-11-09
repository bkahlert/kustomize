package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.imgcstmzr.runtime.HasStatus

open class MutedBlockRenderingLogger<R>(
    caption: CharSequence = "",
    borderedOutput: Boolean = false,
    interceptor: (CharSequence) -> CharSequence? = { it },
    log: (CharSequence) -> Any = { },
) : BlockRenderingLogger<R>(
    caption = caption,
    borderedOutput = borderedOutput,
    interceptor = interceptor,
    log = log) {

    override fun render(trailingNewline: Boolean, block: () -> CharSequence) {}

    override fun logStatus(items: List<HasStatus>, block: () -> IO) = this

    override fun logResult(block: () -> Result<R>): R = block().getOrThrow()

    override fun toString(): String = "MutedBlockRenderingLogger()"
}
