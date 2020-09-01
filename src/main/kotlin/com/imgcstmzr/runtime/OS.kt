package com.imgcstmzr.runtime

import com.imgcstmzr.process.Output
import java.nio.file.Path

enum class SupportedOS : () -> OS<Workflow> {
    RPI_LITE {
        override fun invoke(): OS<Workflow> = RaspberryPiOSLite()
    };

    val downloadUrl: String? = invoke().downloadUrl
}

interface RunningOS<P : Program<*>> {
    /**
     * Forwards the [values] to the OS running process.
     */
    fun input(vararg values: String)

    /**
     * Logs the current execution status given the [output] and [unfinishedPrograms].
     */
    fun status(output: Output, unfinishedPrograms: List<P>)

    /**
     * Initiates the systems immediate shutdown.
     */
    fun shutdown(): Unit

    val shuttingDown: Boolean
}

/**
 * Representation of an operating system that can be customized.
 */
interface OS<P : Program<*>> {

    /**
     * URL that allows an image of this [OS] to be downloaded.
     */
    val downloadUrl: String?

    fun increaseDiskSpace(
        size: Long,
        img: Path,
        runtime: Runtime<P>,
    ): Int;

    fun bootAndRun(
        scencario: String,
        img: Path,
        runtime: Runtime<P>,
        processor: RunningOS<P>.(Output) -> Unit,
    ): Int

    /**
     * Compiles a special script with a [name] that consists itself of multiple scripts
     * in the form of labeled [commandBlocks].
     *
     * A command block starts with a header (e.g. `: label`) followed by a script.
     *
     * Example:
     * ```shell script
     * : say "Hello\nWorld""
     * echo "Hello"
     * echo "World"
     * ```
     */
    fun compileSetupScript(name: String, commandBlocks: String): Array<Workflow>

    /**
     * Compiles a script with a [name] consisting of [commands] to be executed.
     */
    fun compileScript(name: String, vararg commands: String): Workflow
}
