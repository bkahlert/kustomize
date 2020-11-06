package com.imgcstmzr.process

import com.bkahlert.koodies.concurrent.process.CompletedProcess
import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.concurrent.process.RunningProcess
import com.bkahlert.koodies.nio.NonBlockingReader
import java.io.IOException
import java.io.UncheckedIOException
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
class CommandLineRunner() {
    private var log: ((String) -> Unit)? = null

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
//        test: String,
        processor: Process.(IO) -> Unit,
    ): RunningProcess {

        println("OLD: " + directory + " --- " + shellScript)
//        val split = test.split(";")
//        split.forEach {
//            val new = Exec.commandLine(it, emptyArray(), null, emptyMap())
//            println("NEW: " + new.executable + " --- " + new.arguments.toList())
//            println("NEW: " + new.commandline.toList())
//            println("NEW: " + new)
//
//            val process = try {
//                log?.invoke("Starting $shellScript in $directory...")
//                val process = ProcessBuilder()
//                    .command(*new.commandline)
//                    .directory(new.workingDirectory)
//                    .redirectInput(PIPE)
//                    .redirectOutput(PIPE)
//                    .redirectError(PIPE)
//                    .start()
//                log?.invoke("Process $process successfully started")
//                process
//            } catch (e: IOException) {
//                throw UncheckedIOException(e)
//            }
//            log = { line -> process.processor(META typed line) }
//            NonBlockingReader(process.inputStream).forEachLine { line -> process.processor(OUT typed line) }
//            NonBlockingReader(process.errorStream).forEachLine { line -> process.processor(ERR typed line) }
//
//
//            System.exit(0)
//        }


        val process = try {
            log?.invoke("Starting $shellScript in $directory...")
            val process = ProcessBuilder()
                .command(SHELL_CMD, SHELL_CMD_ARGS, shellScript)
//                .command(new.)
                .directory(directory.toAbsolutePath().toFile())
                .redirectInput(PIPE)
                .redirectOutput(PIPE)
                .redirectError(PIPE)
                .start()
            log?.invoke("Process $process successfully started")
            process
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
        log = { line -> process.processor(META typed line) }
        NonBlockingReader(process.inputStream).forEachLine { line -> process.processor(OUT typed line) }
        NonBlockingReader(process.errorStream).forEachLine { line -> process.processor(ERR typed line) }
        return process.waitForProcessAsync()
    }

    private fun Process.waitForProcessAsync(): RunningProcess =
        object : RunningProcess() {
            override val process: Process = this
            override val result: CompletableFuture<CompletedProcess> = CompletableFuture.supplyAsync { CompletedProcess(0L, waitForProcess(), emptyList()) }
        }

    private fun Process.waitForProcess(): Int {
        try {
            log?.invoke("Waiting for process $this to complete...")
            val exitCode = waitFor()
            log?.invoke("$this exited with code $exitCode")
            return exitCode
        } catch (ie: InterruptedException) {
            log?.invoke("Waiting for process completion was interrupted. Destroying $this...")
            destroyForcibly()
            Thread.currentThread().interrupt()
            while (true) {
                try {
                    log?.invoke("Waiting for process destruction of $this to finish...")
                    val exitCode = waitFor()
                    log?.invoke("Process $this successfully destructed with code $exitCode")
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
