package koodies.ext.concurrent

import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processors
import koodies.concurrent.script
import koodies.logging.RenderingLogger
import koodies.shell.ShellScript

fun script(
    logger: RenderingLogger?,
    environment: Map<String, String> = emptyMap(),
    expectedExitValue: Int? = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess {
    val processor = logger?.let { Processors.loggingProcessor(it) } ?: Processors.noopProcessor()
    return script(processor = processor, shellScript = shellScript)
}
