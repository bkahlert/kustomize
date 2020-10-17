package com.imgcstmzr.runtime.log

import com.imgcstmzr.process.Output
import com.imgcstmzr.runtime.HasStatus

open class MutedBlockRenderingLogger<R>(
    caption: String = "",
    borderedOutput: Boolean = false,
    interceptor: (String) -> String? = { it },
    log: (String) -> Unit = { },
) : BlockRenderingLogger<R>(caption, borderedOutput, interceptor, log) {

    override fun logLambda(trailingNewline: Boolean, block: () -> String) {}

    override fun logLineLambda(items: List<HasStatus>, block: () -> Output) = this

    override fun logLastLambda(block: () -> Result<R>): R = block().getOrThrow()
}
