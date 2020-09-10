package com.imgcstmzr.process

import com.imgcstmzr.util.hereDoc
import java.io.BufferedWriter
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.io.OutputStreamWriter
import java.lang.ProcessBuilder.Redirect
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.lang.ProcessBuilder.Redirect.PIPE
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.stream.Stream

fun runProcess(
    vararg args: String,
    blocking: Boolean = true,
    inputRedirect: Redirect = PIPE,
    outputRedirect: Redirect = INHERIT,
    errorRedirect: Redirect = PIPE,
    processor: Process.(Output) -> Unit,
): RunningProcess {
    val cmd: String = args.joinToString(" ")
    return CommandLineRunner(blocking)
        .startProcessAndWaitForCompletion(
            File("/bin").toPath(),
            "sh " + hereDoc(listOf(cmd)),
            inputRedirect,
            outputRedirect,
            errorRedirect,
            processor,
        )
}

fun runRawProcess(
    directory: Path,
    command: List<String>,
    blocking: Boolean = true,
    inputRedirect: Redirect = PIPE,
    outputRedirect: Redirect = INHERIT,
    errorRedirect: Redirect = PIPE,
    processor: Process.(Output) -> Unit,
): RunningProcess {
    return CommandLineRunner(blocking)
        .startRawProcessAndWaitForCompletion(
            directory,
            command,
            inputRedirect,
            outputRedirect,
            errorRedirect,
            processor,
        )
}

fun runDocker(
    containerName: String,
    vararg args: String,
    blocking: Boolean = true,
    inputRedirect: Redirect = PIPE,
    outputRedirect: Redirect = INHERIT,
    errorRedirect: Redirect = PIPE,
    processor: Process.(Output) -> Unit,
): RunningProcess {
    val dockerRun = arrayOf(
        "docker",
        "run",
        "--name", "\"$containerName\"",
        "--rm",
        *args
    ).joinToString(" ")
    val cmd = "docker rm --force \"$containerName\" &> /dev/null ; $dockerRun"
    return runProcess(args = arrayOf(cmd), blocking, inputRedirect, outputRedirect, errorRedirect, processor)
}


@Suppress("KDocMissingDocumentation")
open class RunningProcess(val process: Process, private val futureResult: CompletableFuture<Int>) : Process() {
    override fun getOutputStream(): OutputStream = process.outputStream

    override fun getInputStream(): InputStream = process.inputStream

    override fun getErrorStream(): InputStream = process.errorStream

    override fun exitValue(): Int = process.exitValue()

    override fun destroy(): Unit = process.destroy()

    override fun destroyForcibly(): Process = process.destroyForcibly()

    override fun supportsNormalTermination(): Boolean = process.supportsNormalTermination()

    override fun waitFor(timeout: Long, unit: TimeUnit?): Boolean = process.waitFor(timeout, unit)

    override fun isAlive(): Boolean = process.isAlive

    override fun pid(): Long = process.pid()

    override fun onExit(): CompletableFuture<Process> = process.onExit()

    override fun toHandle(): ProcessHandle = process.toHandle()

    override fun info(): ProcessHandle.Info = process.info()

    override fun children(): Stream<ProcessHandle> = process.children()

    override fun descendants(): Stream<ProcessHandle> = process.descendants()

    override fun waitFor(): Int = process.waitFor()

    val failed: Boolean
        get() = futureResult.isCancelled && blockExitCode != 0

    val blockExitCode: Int
        get() = futureResult.get()
}

/**
 * Returns this [Process] if it is not `null` and alive.
 *
 * @throws IllegalStateException
 */
fun <T : Process> T?.alive(): T = this
    ?.also { check(it.isAlive) { "Process is not alive." } }
    ?: throw IllegalStateException("Process does not exist.")

/**
 * Write the given [input] strings with a slight delay between
 * each input on the [Process]'s [InputStream].
 */
fun Process.input(vararg input: String) {
    val stdin = BufferedWriter(OutputStreamWriter(this.outputStream))
    input.forEach {
        TimeUnit.MILLISECONDS.sleep(10)
        stdin.write(it)
        stdin.flush()
    }
}

/**
 * Enters the given [input] by writing it on the [Process]'s [InputStream] as
 * if it was a user's input sent by a hit of the enter key.
 */
fun Process.enter(vararg input: String) {
    input("\r")
}
