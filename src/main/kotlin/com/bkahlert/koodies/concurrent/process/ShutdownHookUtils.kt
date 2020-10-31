package com.bkahlert.koodies.concurrent.process

import java.security.AccessControlException
import kotlin.concurrent.thread

object ShutdownHookUtils {
    fun processHookFor(process: Process) = thread(start = false, name = "process shutdown hook", contextClassLoader = null) { process.destroy() }
    fun addShutDownHook(hook: Thread): Any = kotlin.runCatching { Runtime.getRuntime().addShutdownHook(hook) }.onFailure { it.rethrowIfUnexpected() }
    fun removeShutdownHook(hook: Thread): Any = kotlin.runCatching { Runtime.getRuntime().removeShutdownHook(hook) }.onFailure { it.rethrowIfUnexpected() }
    private fun Throwable.rethrowIfUnexpected(): Any = if (this !is IllegalStateException && this !is AccessControlException) throw this else Unit
}
