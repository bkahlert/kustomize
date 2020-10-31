package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.imgcstmzr.runtime.HasStatus

open class MutedBlockRenderingLogger<R>(
    caption: String = "",
    borderedOutput: Boolean = false,
    interceptor: (String) -> String? = { it },
    log: (String) -> Any = { },
) : BlockRenderingLogger<R>(caption, borderedOutput, interceptor, log) {

    override fun render(trailingNewline: Boolean, block: () -> String) {}

    override fun logStatus(items: List<HasStatus>, block: () -> IO) = this

    override fun logResult(block: () -> Result<R>): R = block().getOrThrow()

    override fun toString(): String = "MutedBlockRenderingLogger()"
}
