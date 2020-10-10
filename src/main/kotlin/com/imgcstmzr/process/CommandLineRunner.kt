package com.imgcstmzr.process

import com.bkahlert.koodies.nio.NonBlockingReader
import com.github.ajalt.clikt.output.TermUi
import com.imgcstmzr.process.Output.Type.ERR
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.runtime.log.InfoAdaptingLogger
import com.imgcstmzr.util.debug
import org.slf4j.Logger
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.UncheckedIOException
import java.lang.ProcessBuilder.Redirect
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.lang.ProcessBuilder.Redirect.PIPE
import java.nio.file.Path
import java.util.Locale
import java.util.concurrent.CompletableFuture
import kotlin.time.ExperimentalTime


/**
 * Tool that allows to run a shell script without having to hassle with
 * problems concerning output redirection and synchronization.
 */
@Deprecated("Replace by Exec")
class CommandLineRunner(private var blocking: Boolean = true) {
    private var log: Logger? = null

    /**
     * Starts a process and waits for its return while using the provided Consumer for all three forms
     * of output:
     *
     *  1. Status messages triggered by command line runner
     *  1. Output of the running process
     *  1. Errors of the running process
     *
     * @param directory the path where the script resides
     * @param shellScript the file name of the script
     * @param processor consumer to which all generated output will be distinguishably forwarded
     *
     * @return completed future that blocks on access in case the process has not terminated execution yet
     */
    @OptIn(ExperimentalTime::class)
    fun startProcessAndWaitForCompletion(
        directory: Path,
        shellScript: String,
        inputRedirect: Redirect = PIPE,
        outputRedirect: Redirect = INHERIT,
        errorRedirect: Redirect = PIPE,
        processor: Process.(Output) -> Unit,
    ): RunningProcess {
        val loggerName = CommandLineRunner::class.java.simpleName + "(" + shellScript.split(" ").toTypedArray()[0] + ")"
        val process = startProcess(directory, shellScript, inputRedirect, outputRedirect, errorRedirect)
        log = InfoAdaptingLogger(loggerName) { line -> process.processor(META typed line) }
        if (blocking) {
            BufferedReader(InputStreamReader(process.inputStream)).forEachLine { line -> process.processor(OUT typed line) }
            BufferedReader(InputStreamReader(process.errorStream)).forEachLine { line -> process.processor(ERR typed line) }
        } else {
            NonBlockingReader(process.inputStream).forEachLine { line -> process.processor(OUT typed line) }
            NonBlockingReader(process.errorStream).forEachLine { line -> process.processor(ERR typed line) }
        }
        return waitForProcessAsync(process)
    }

    private fun startProcess(
        directory: Path,
        shellScript: String,
        inputRedirect: Redirect = PIPE,
        outputRedirect: Redirect = INHERIT,
        errorRedirect: Redirect = PIPE,
    ): Process {
        return try {
            log?.info("Starting {}...", shellScript)
            TermUi.debug("$directory\$ $shellScript")
            val process = ProcessBuilder()
                .command(SHELL_CMD, SHELL_CMD_ARGS, shellScript)
                .directory(directory.toAbsolutePath().toFile())
                .redirectInput(inputRedirect)
                .redirectOutput(outputRedirect)
                .redirectError(errorRedirect)
                .start()
            log?.info("Process {} successfully started", process)
            process
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    fun startRawProcessAndWaitForCompletion(
        directory: Path,
        command: List<String>,
        inputRedirect: Redirect = PIPE,
        outputRedirect: Redirect = INHERIT,
        errorRedirect: Redirect = PIPE,
        processor: Process.(Output) -> Unit,
    ): RunningProcess {
        val loggerName = CommandLineRunner::class.java.simpleName + "(" + command.toTypedArray()[0] + ")"
        val process = startRawProcess(directory, command, inputRedirect, outputRedirect, errorRedirect)
        log = InfoAdaptingLogger(loggerName) { line -> process.processor(META typed line) }
        if (blocking) {
            BufferedReader(InputStreamReader(process.inputStream)).forEachLine { line -> process.processor(OUT typed line) }
            BufferedReader(InputStreamReader(process.errorStream)).forEachLine { line -> process.processor(ERR typed line) }
        } else {
            NonBlockingReader(process.inputStream).forEachLine { line -> process.processor(OUT typed line) }
            NonBlockingReader(process.errorStream).forEachLine { line -> process.processor(ERR typed line) }
        }
        return waitForProcessAsync(process)
    }

    private fun startRawProcess(
        directory: Path,
        command: List<String>,
        inputRedirect: Redirect = PIPE,
        outputRedirect: Redirect = INHERIT,
        errorRedirect: Redirect = PIPE,
    ): Process {
        return try {
            log?.info("Starting {}...", command)
            TermUi.debug(command)
            val process = ProcessBuilder()
                .command(command)
                .directory(directory.toFile())
                .redirectInput(inputRedirect)
                .redirectOutput(outputRedirect)
                .redirectError(errorRedirect)
                .start()
            log?.info("Process {} successfully started", process)
            process
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    private fun waitForProcessAsync(process: Process): RunningProcess = RunningProcess(process, CompletableFuture.supplyAsync { waitForProcess(process) })
    private fun waitForProcess(process: Process): Int {
        try {
            log?.info("Waiting for process {} to complete...", process)
            val exitCode = process.waitFor()
            log?.info("{} exited with code {}", process, exitCode)
            return exitCode
        } catch (ie: InterruptedException) {
            log?.info("Waiting for process completion was interrupted. Destroying {}...", process)
            process.destroyForcibly()
            Thread.currentThread().interrupt()
            while (true) {
                try {
                    log?.info("Waiting for process destruction to finish...", process)
                    val exitCode = process.waitFor()
                    log?.info("Process successfully destructed with code {}", process, exitCode)
                    return exitCode
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                }
            }
        }
    }

    companion object {
        private val IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).startsWith("windows")
        private val SHELL_CMD = if (IS_WINDOWS) "cmd.exe" else "sh"
        private val SHELL_CMD_ARGS = if (IS_WINDOWS) "/c" else "-c"
    }
}
