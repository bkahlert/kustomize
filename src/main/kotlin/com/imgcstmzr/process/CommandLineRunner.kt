package com.imgcstmzr.process

import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.helpers.SubstituteLogger
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.UncheckedIOException
import java.lang.ProcessBuilder.Redirect
import java.lang.ProcessBuilder.Redirect.INHERIT
import java.lang.ProcessBuilder.Redirect.PIPE
import java.nio.file.Path
import java.text.MessageFormat
import java.util.Locale
import java.util.concurrent.CompletableFuture
import java.util.regex.Pattern


//private val confirmationSequence = "         ✌⊂(✰‿✰)つ✌"
//private val confirmationSequenceCommand = "\recho '$confirmationSequence'\r"
//fun Process.fakeInputAndConfirm(vararg input: String) {
//    fakeInput(*input, confirmationSequenceCommand)
//}


/**
 * Tool that allows to run a shell script without having to hassle with
 * problems concerning output redirection and synchronization.
 */
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
     *
     * @param directory the path where the script resides
     * @param shellScript the file name of the script
     * @param processOutputProcessor consumer to which all generated output will be forwarded
     *
     * @return completed future that blocks on access in case the process has not terminated execution yet
     */
    fun startProcessAndWaitForCompletion(
        directory: Path,
        shellScript: String,
        inputRedirect: Redirect = PIPE,
        outputRedirect: Redirect = INHERIT,
        errorRedirect: Redirect = PIPE,
        processOutputProcessor: (String) -> Unit,
    ): CompletableFuture<Int> = startProcessAndWaitForCompletion(directory, shellScript, inputRedirect, outputRedirect, errorRedirect)
    { process: Process, origin: Origin, line: String -> processOutputProcessor.invoke(line) }

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
     * @param processOutputProcessor consumer to which all generated output will be distinguishably forwarded
     *
     * @return completed future that blocks on access in case the process has not terminated execution yet
     */
    fun startProcessAndWaitForCompletion(
        directory: Path,
        shellScript: String,
        inputRedirect: Redirect = PIPE,
        outputRedirect: Redirect = INHERIT,
        errorRedirect: Redirect = PIPE,
        processOutputProcessor: (Process, Origin, String) -> Unit,
    ): CompletableFuture<Int> {
        val loggerName = CommandLineRunner::class.java.simpleName + "(" + shellScript.split(" ").toTypedArray()[0] + ")"
        val process = startProcess(directory, shellScript, inputRedirect, outputRedirect, errorRedirect)
        log = InfoConsumingLogger(loggerName) { line -> processOutputProcessor.invoke(process, Origin.STATUS, line) }
        if (blocking) {
            BufferedReader(InputStreamReader(process.inputStream)).forEachLine { line -> processOutputProcessor.invoke(process, Origin.OUT, line) }
            BufferedReader(InputStreamReader(process.errorStream)).forEachLine { line -> processOutputProcessor.invoke(process, Origin.ERR, line) }
        } else {
            NonBlockingReader(process, { inputStream }).forEachLine { line -> processOutputProcessor.invoke(process, Origin.OUT, line) }
            NonBlockingReader(process, { errorStream }).forEachLine { line -> processOutputProcessor.invoke(process, Origin.ERR, line) }
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
        val absoluteShellScript = directory.resolve(shellScript)
        return try {
            log?.info("Starting {}...", absoluteShellScript)
            val process = ProcessBuilder()
                .command(SHELL_CMD, SHELL_CMD_ARGS, absoluteShellScript.toAbsolutePath().toString())
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

    private fun waitForProcessAsync(process: Process): CompletableFuture<Int> = CompletableFuture.supplyAsync { waitForProcess(process) }

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

    enum class Origin {
        /**
         * Status messages from the runner itself
         */
        STATUS,

        /**
         * Redirected standard output of the called process
         */
        OUT,

        /**
         * Redirected error output of the called process
         */
        ERR
    }

    companion object {
        private val IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).startsWith("windows")
        private val SHELL_CMD = if (IS_WINDOWS) "cmd.exe" else "sh"
        private val SHELL_CMD_ARGS = if (IS_WINDOWS) "/c" else "-c"
    }
}


private const val SLF4J_ANCHOR = "{}"
private val SLF4J_PATTERN = Pattern.compile(Pattern.quote(SLF4J_ANCHOR))
private const val MESSAGE_FORMAT_REPLACEMENT = "{%d}"

/**
 * `slf4jFormat("This is a log {} with {} {}", "message", "some", "parameters") == "This is a log message with some parameters"`
 */
fun slf4jFormat(slf4jLogMessage: String, vararg args: Any): String {
    var messageFormatPattern = slf4jLogMessage
    var index = 0

    var matcher = SLF4J_PATTERN.matcher(messageFormatPattern)
    while (matcher.find()) {
        messageFormatPattern = matcher.replaceFirst(String.format(MESSAGE_FORMAT_REPLACEMENT, index))
        matcher = SLF4J_PATTERN.matcher(messageFormatPattern)
        index++
    }
    val messageFormat = MessageFormat(messageFormatPattern)
    return messageFormat.format(args, StringBuffer(slf4jLogMessage.length shl 1), null).toString()
}

/**
 * Logger that redirects all INFO logs to the given consumer.
 */
private class InfoConsumingLogger(name: String, private val consumer: (String) -> Unit) : SubstituteLogger(name, null, true) {
    override fun isInfoEnabled(): Boolean = true

    override fun info(msg: String) {
        consumer.invoke(msg)
    }

    override fun info(format: String, arg: Any) {
        consumer.invoke(slf4jFormat(format, arg))
    }

    override fun info(format: String, arg1: Any, arg2: Any) {
        consumer.invoke(slf4jFormat(format, arg1, arg2))
    }

    override fun info(format: String, vararg arguments: Any) {
        consumer.invoke(slf4jFormat(format, arguments))
    }

    override fun info(msg: String, t: Throwable) {
        consumer.invoke(slf4jFormat(msg, t))
    }

    override fun isInfoEnabled(marker: Marker): Boolean = true
}
