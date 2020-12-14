package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.imgcstmzr.runtime.HasStatus

/**
 * A logger that can be used if no logging is needed.
 */
open class MutedRenderingLogger : BlockRenderingLogger("", log = {}) {

    override fun logText(block: () -> CharSequence) {}
    override fun logLine(block: () -> CharSequence) {}
    override fun logStatus(items: List<HasStatus>, block: () -> IO) {}
    override fun logException(block: () -> Throwable) {}
    override fun <R> logResult(block: () -> Result<R>): R = block().getOrThrow()
    override fun toString(): String = "MutedBlockRenderingLogger"
}