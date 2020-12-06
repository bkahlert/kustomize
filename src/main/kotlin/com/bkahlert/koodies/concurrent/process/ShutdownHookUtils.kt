package com.bkahlert.koodies.concurrent.process

import java.security.AccessControlException
import kotlin.concurrent.thread

object ShutdownHookUtils {
    @Deprecated("handlded by DelegatingProcess")
    fun processHookFor(process: Process): Thread =
        thread(start = false, name = "process shutdown hook", contextClassLoader = null) { process.destroy() }

    fun <T : () -> Unit> addShutDownHook(hook: T): T = hook.also { addShutDownHook(thread(start = false) { hook() }) }
    fun addShutDownHook(hook: Thread): Any = kotlin.runCatching { Runtime.getRuntime().addShutdownHook(hook) }.onFailure { it.rethrowIfUnexpected() }
    fun removeShutdownHook(hook: Thread): Any =
        kotlin.runCatching { Runtime.getRuntime().removeShutdownHook(hook) }.onFailure { it.rethrowIfUnexpected() }

    private fun Throwable.rethrowIfUnexpected(): Any = if (this !is IllegalStateException && this !is AccessControlException) throw this else Unit
}
