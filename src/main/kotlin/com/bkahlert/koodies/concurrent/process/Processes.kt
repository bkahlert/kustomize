package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.cleanUpOldTempFiles
import com.bkahlert.koodies.concurrent.cleanUpOnShutdown
import com.bkahlert.koodies.concurrent.process.ShellScript.Companion.build
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.shell.shebang
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.imgcstmzr.util.Paths.WORKING_DIRECTORY
import org.codehaus.plexus.util.cli.Commandline
import java.io.InputStream
import java.nio.file.Path
import kotlin.time.minutes

/**
 * Provides methods to start a new [Process] and to access running ones.
 */
object Processes {

    private const val shellScriptPrefix: String = "koodies.process."
    private const val shellScriptExtension: String = ".sh"

    /**
     * Builds a proper script that runs at [workingDirectory] and saved it as a
     * temporary file (to be deleted once in a while).
     */
    internal fun buildShellScriptToTempFile(
        workingDirectory: Path,
        shellScriptBuilder: ShellScript.() -> Unit,
    ): Path {
        val shellScriptLines = shellScriptBuilder.build().lines
        val shellScriptWithWorkingDirectory = ShellScript().apply {
            shebang
            changeDirectoryOrExit(directory = workingDirectory)
            shellScriptLines.forEach { line(it) }
        }
        return shellScriptWithWorkingDirectory.buildTo(tempFile(base = shellScriptPrefix, extension = shellScriptExtension))
    }

    /**
     * Builds a proper script that runs at [workingDirectory] and saved it as a
     * temporary file (to be deleted once in a while).
     */
    internal fun buildShellScriptToTempFile(
        workingDirectory: Path?,
        command: Command,
    ): Path = ShellScript().apply {
        shebang
        if (workingDirectory != null) changeDirectoryOrExit(directory = workingDirectory)
        command(command)
    }.buildTo(tempFile(base = shellScriptPrefix, extension = shellScriptExtension))

    init {
        cleanUpOldTempFiles(shellScriptPrefix, shellScriptExtension, minAge = 30.minutes, keepAtMost = 200)
    }

    /**
     * Runs the [shellScript] asynchronously and with no helping wrapper.
     *
     * Returns the raw [Process].
     */
    fun startShellScriptDetached(
        workingDirectory: Path = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        shellScript: ShellScript.() -> Unit,
    ): Process {
        val command = ShellScript().apply {
            shebang
            changeDirectoryOrExit(directory = workingDirectory)
            shellScript()
        }.buildTo(tempFile(base = shellScriptPrefix, extension = shellScriptExtension)).cleanUpOnShutdown().serialized
        return Commandline(command).apply {
            addArguments(arguments)
            @Suppress("ExplicitThis")
            this.workingDirectory = workingDirectory.toFile()
            env.forEach { addEnvironment(it.key, it.value) }
        }.execute()
    }

    /**
     * Same as [evalShellScript] but reads `std output` synchronously
     * with neither additional comfort nor additional threads overhead.
     */
    fun cheapEvalShellScript(
        workingDirectory: Path = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        shellScript: ShellScript.() -> Unit,
    ): String =
        startShellScriptDetached(workingDirectory, env, shellScript)
            .inputStream.bufferedReader().readText()
            .removeEscapeSequences()
            .trim()

    /**
     * Runs the [command] synchronously in a lightweight fashion and returns if the [substring] is contained in the output.
     */
    fun checkIfOutputContains(command: String, substring: String, caseSensitive: Boolean = false): Boolean = runCatching {
        val flags = if (caseSensitive) "" else "i"
        check(startShellScriptDetached { line("$command | grep -q$flags '$substring'") }.waitFor() == 0)
    }.isSuccess

    /**
     * Runs the [shellScriptBuilder] synchronously and returns the [LoggedProcess].
     */
    fun evalShellScript(
        workingDirectory: Path = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        inputStream: InputStream? = null,
        shellScriptBuilder: ShellScript.() -> Unit,
    ): LoggedProcess {
        return startShellScript(
            workingDirectory = workingDirectory,
            env = env,
            inputStream = inputStream,
            shellScriptBuilder = shellScriptBuilder,
        ).loggedProcess.get()
    }

    /**
     * Runs the [shellScriptBuilder] asynchronously and returns the [LoggingProcess].
     */
    fun startShellScript(
        workingDirectory: Path = WORKING_DIRECTORY,
        env: Map<String, String> = emptyMap(),
        runAfterProcessTermination: (() -> Unit)? = null,
        inputStream: InputStream? = null,
        ioProcessor: (LoggingProcess.(IO) -> Unit)? = null,
        shellScriptBuilder: ShellScript.() -> Unit,
    ): LoggingProcess = LoggingProcess(
        command = buildShellScriptToTempFile(workingDirectory, shellScriptBuilder).serialized,
        arguments = emptyList(),
        workingDirectory = null, // part of the shell script
        env = env,
        runAfterProcessTermination = runAfterProcessTermination,
        userProvidedInputStream = inputStream,
        ioProcessor = ioProcessor,
    )
}
