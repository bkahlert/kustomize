package com.bkahlert.koodies.concurrent.process

import java.util.concurrent.CompletableFuture
import java.lang.Process as JavaProcess

/**
 * Lightweight process that simply read stdout.
 */
open class LightweightProcess(
    protected val commandLine: CommandLine,
) : ProcessDelegate() {

    override val javaProcess: JavaProcess by lazy {
        commandLine.lazyProcess().value
    }

    val output: String by lazy { inputStream.bufferedReader().use { it.readText() }.trim() }

    override val onExit: CompletableFuture<Process>
        get() = javaProcess.onExit().thenApply { this }

    init {
        waitForTermination()
    }
}
