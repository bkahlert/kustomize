package com.imgcstmzr

import com.github.ajalt.clikt.output.TermUi
import org.slf4j.Logger
import org.slf4j.Marker
import org.slf4j.helpers.SubstituteLogger
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.UncheckedIOException
import java.nio.file.Files
import java.nio.file.Path
import java.text.MessageFormat
import java.util.Locale
import java.util.Objects
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.regex.Pattern

class Downloader {
    fun download(url: String): File {
        val temp = Files.createTempDirectory("imgcstmzr")

        val cmd = listOf(
            "wget",
            "--tries=10",
            "--timeout=15",
            "--waitretry=5",
            "--retry-connrefused",
            "--compression=auto",
            "--content-disposition",
            "--continue",
            "--ignore-case",
            "--adjust-extension",
            "--directory-prefix=\"$temp\"",
            "\"$url\""
        ).joinToString(" ")

        CommandLineRunner().startProcessAndWaitForCompletion(
            File("/bin").toPath(), "sh -c '$cmd'"
        ) { str ->
            TermUi.echo(str)
        }.get()

        return temp.toFile()?.listFiles()?.first()
            ?: throw IllegalStateException("An unknown error occurred while downloading. $temp was supposed to contain the downloaded file but was empty.")
    }
}

class Unarchiver {
    fun unarchive(archive: File): File {
        val temp = Files.createTempDirectory("imgcstmzr")
        val cmd = listOf(
            "tar",
            "-xvf",
            "\"$archive\"",
            "-C",
            "\"$temp\""
        ).joinToString(" ")

        CommandLineRunner().startProcessAndWaitForCompletion(
            File("/bin").toPath(), "sh -c '$cmd'"
        ) { str ->
            TermUi.echo(str)
        }.get()

        return temp.toFile()?.listFiles()?.first()
            ?: throw IllegalStateException("An unknown error occurred while unarchiving. $temp was supposed to contain the unarchived file but was empty.")
    }
}

/**
 * Tool that allows to run a shell script without having to hassle with
 * problems concerning output redirection and synchronization.
 *
 * @author Björn Kahlert
 */
private class CommandLineRunner {
    private var log: Logger? = null

    /**
     * Starts a process and waits for its return while using the provided Consumer for all three forms
     * of output:
     *
     *  1. Status messages triggered by command line runner
     *  1. Output of the running process
     *  1. Errros of the running process
     *
     *
     * @param directory the path where the script resides
     * @param shellScript the file name of the script
     * @param allInOneConsumer consumer to which all generated output will be forwarded
     *
     * @return completed future that blocks on access in case the process has not terminated execution yet
     */
    fun startProcessAndWaitForCompletion(directory: Path, shellScript: String, allInOneConsumer: Consumer<String?>): CompletableFuture<Int> {
        return startProcessAndWaitForCompletion(
            directory, shellScript
        ) { origin: Origin?, line: String? -> allInOneConsumer.accept(line) }
    }

    /**
     * Starts a process and waits for its return while using the provided Consumer for all three forms
     * of output:
     *
     *  1. Status messages triggered by command line runner
     *  1. Output of the running process
     *  1. Errros of the running process
     *
     *
     * @param directory the path where the script resides
     * @param shellScript the file name of the script
     * @param originConsumer consumer to which all generated output will be distinguishably forwarded
     *
     * @return completed future that blocks on access in case the process has not terminated execution yet
     */
    fun startProcessAndWaitForCompletion(directory: Path, shellScript: String, originConsumer: BiConsumer<Origin?, String?>): CompletableFuture<Int> {
        val loggerName = CommandLineRunner::class.java.simpleName + "(" + shellScript.split(" ").toTypedArray()[0] + ")"
        log = InfoConsumingLogger(loggerName) { line -> originConsumer.accept(Origin.STATUS, line) }
        val process = startProcess(directory, shellScript, ProcessBuilder.Redirect.PIPE, ProcessBuilder.Redirect.PIPE)
        Thread(StreamGobbler(
            process.inputStream
        ) { line: String? -> originConsumer.accept(Origin.OUT, line) }).start()
        Thread(StreamGobbler(
            process.errorStream
        ) { line: String? -> originConsumer.accept(Origin.ERR, line) }).start()
        return waitForProcessAsync(process)
    }

    private fun startProcess(directory: Path, shellScript: String, outputRedirect: ProcessBuilder.Redirect, errorRedirect: ProcessBuilder.Redirect): Process {
        val absoluteShellScript = directory.resolve(shellScript)
        return try {
            log!!.info("Starting {}...", absoluteShellScript)
            val process = ProcessBuilder()
                .command(SHELL_CMD, SHELL_CMD_ARGS, absoluteShellScript.toAbsolutePath().toString())
                .directory(directory.toAbsolutePath().toFile())
                .redirectInput(ProcessBuilder.Redirect.PIPE)
                .redirectOutput(outputRedirect)
                .redirectError(errorRedirect)
                .start()
            log!!.info("Process {} successfully started", process)
            process
        } catch (e: IOException) {
            throw UncheckedIOException(e)
        }
    }

    private fun waitForProcessAsync(process: Process): CompletableFuture<Int> {
        return CompletableFuture.supplyAsync { waitForProcess(process) }
    }

    private fun waitForProcess(process: Process): Int {
        try {
            log!!.info("Waiting for process {} to complete...", process)
            val exitCode = process.waitFor()
            log!!.info("{} exited with code {}", process, exitCode)
            return exitCode
        } catch (ie: InterruptedException) {
            log!!.info("Waiting for process completion was interrupted. Destroying {}...", process)
            process.destroyForcibly()
            Thread.currentThread().interrupt()
            while (true) {
                try {
                    log!!.info("Waiting for process destruction to finish...", process)
                    val exitCode = process.waitFor()
                    log!!.info("Process successfully destructed with code {}", process, exitCode)
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

    private class StreamGobbler(private val inputStream: InputStream, private val inputLineConsumer: Consumer<String?>) : Runnable {
        override fun run() {
            BufferedReader(InputStreamReader(inputStream)).lines().forEach(inputLineConsumer)
        }
    }

    companion object {
        private val IS_WINDOWS = System.getProperty("os.name").toLowerCase(Locale.ENGLISH).startsWith("windows")
        private val SHELL_CMD = if (IS_WINDOWS) "cmd.exe" else "sh"
        private val SHELL_CMD_ARGS = if (IS_WINDOWS) "/c" else "-c"
    }
}


/**
 * Logger that redirects all INFO logs to the given consumer.
 *
 * @author Björn Kahlert
 */
private class InfoConsumingLogger(name: String?, private val consumer: Consumer<String>) : SubstituteLogger(name, null, true) {
    override fun isInfoEnabled(): Boolean {
        return true
    }

    override fun info(msg: String) {
        consumer.accept(msg)
    }

    override fun info(format: String, arg: Any) {
        consumer.accept(StructuredLogging.formatForException(format, arg))
    }

    override fun info(format: String, arg1: Any, arg2: Any) {
        consumer.accept(StructuredLogging.formatForException(format, arg1, arg2))
    }

    override fun info(format: String, vararg arguments: Any) {
        consumer.accept(StructuredLogging.formatForException(format, arguments))
    }

    override fun info(msg: String, t: Throwable) {
        consumer.accept(StructuredLogging.formatForException(msg, t))
    }

    override fun isInfoEnabled(marker: Marker): Boolean {
        return true
    }
}

private class StructuredLogging {

    companion object {
        private const val SLF4J_ANCHOR = "{}"
        private const val MESSAGE_FORMAT_REPLACEMENT = "{%d}"


        /**
         * Formats the given SL4JF formatted message using [MessageFormat].
         * This approach is especially useful for exception messages.
         *
         * <dl>
         * <dt>**Example Part 1: Formatting a log message using SLF4j**</dt>
         * <dd>`log.info("Computation {} succeeded"`, "foo"}.</dd>
         * <dt>**Example Part 2: Formatting a log message for an exception**</dt>
         * <dd>
         * <dl>
         * <dt>Using manual concatenation</dt>
         * <dd>`new RuntimeException("Computation " + computationName + " failed`</dd>
         * <dt>Using [MessageFormat]</dt>
         * <dd>`new RuntimeException(Message.format("Computation {0} failed", computationName))`</dd>
         * <dt>Using StructuredLogging</dt>
         * <dd>`new RuntimeException(StructuredLogging.formatForException("Computation {} failed", computationName))`</dd>
        </dl> *
        </dd> *
         * <dt>**Example Part 3: Explanation**</dt>
         * <dd>Using this method allows you to only have one log message format. Because of that
         * you can organize and store predefined messages centrally and use them for both SLF4j messages and exception messages.</dd>
        </dl> *
         *
         * @param slf4jLogMessage the message originally intended for SL4J logging
         * @param args the args to be used for the message placeholders
         *
         * @return the fully formatted string for an exception's message
         */
        fun formatForException(slf4jLogMessage: String, vararg args: Any): String {
            Objects.requireNonNull(slf4jLogMessage)
            Objects.requireNonNull(args)
            var messageFormatPattern = slf4jLogMessage
            var index = 0
            val slf4jAnchorPattern = Pattern.compile(Pattern.quote(SLF4J_ANCHOR))
            var matcher = slf4jAnchorPattern.matcher(messageFormatPattern)
            while (matcher.find()) {
                messageFormatPattern = matcher.replaceFirst(String.format(MESSAGE_FORMAT_REPLACEMENT, index))
                matcher = slf4jAnchorPattern.matcher(messageFormatPattern)
                index++
            }
            val messageFormat = MessageFormat(messageFormatPattern)
            return messageFormat.format(args, StringBuffer(slf4jLogMessage.length shl 1), null).toString()
        }
    }
}

