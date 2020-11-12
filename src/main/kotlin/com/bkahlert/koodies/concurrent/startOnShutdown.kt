package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.concurrent.process.ShutdownHookUtils

/**
 * Registers [block] for execution on VM shutdown, which is the moment,
 * the application closes.
 */
fun <T : () -> Unit> startOnShutdown(block: T): T = ShutdownHookUtils.addShutDownHook(block)
