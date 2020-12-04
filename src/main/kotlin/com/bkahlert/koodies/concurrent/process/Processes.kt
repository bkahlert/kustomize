package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.cleanUpOldTempFiles
import com.bkahlert.koodies.concurrent.cleanUpOnShutdown
import com.bkahlert.koodies.nio.file.Paths.WorkingDirectory
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.shell.ShellScript
import com.bkahlert.koodies.shell.ShellScript.Companion.build
import com.bkahlert.koodies.shell.shebang
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import org.codehaus.plexus.util.cli.Commandline
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.time.minutes

/**
 * Provides methods to start a new [Process] and to access running ones.
 */
object Processes {

    private const val shellScriptPrefix: String = "koodies.process."
    private const val shellScriptExtension: String = ".sh"

    init {
        cleanUpOldTempFiles(shellScriptPrefix, shellScriptExtension, minAge = 30.minutes, keepAtMost = 200)
    }

    /**
     * Returns an empty script file location that gets deleted after a certain time.
     */
    fun tempScriptFile(): Path = tempFile(base = shellScriptPrefix, extension = shellScriptExtension)

    /**
     * Returns an empty script file location that gets deleted after a certain time.
     */
    fun Path.isTempScriptFile(): Boolean = name.startsWith(shellScriptPrefix) && name.endsWith(shellScriptExtension)

    /**
     * Runs the [shellScript] asynchronously and with no helping wrapper.
     *
     * Returns the raw [Process].
     */
    fun startShellScriptDetached(
        workingDirectory: Path = WorkingDirectory,
        env: Map<String, String> = emptyMap(),
        shellScript: ShellScript.() -> Unit,
    ): Process {
        val command = ShellScript().apply {
            shebang
            changeDirectoryOrExit(directory = workingDirectory)
            shellScript()
        }.buildTo(tempScriptFile()).cleanUpOnShutdown().serialized
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
        workingDirectory: Path = WorkingDirectory,
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
        workingDirectory: Path = WorkingDirectory,
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
     * Builds a proper script that runs at [workingDirectory] and saved it as a
     * temporary file (to be deleted once in a while).
     */
    private fun buildShellScriptToTempFile(
        workingDirectory: Path,
        shellScriptBuilder: ShellScript.() -> Unit,
    ): Path {
        val shellScriptLines = shellScriptBuilder.build().lines
        val shellScriptWithWorkingDirectory = ShellScript().apply {
            shebang
            changeDirectoryOrExit(directory = workingDirectory)
            shellScriptLines.forEach { line(it) }
        }
        return shellScriptWithWorkingDirectory.buildTo(tempScriptFile())
    }

    /**
     * Runs the [shellScriptBuilder] asynchronously and returns the [LoggingProcess].
     */
    fun startShellScript(
        workingDirectory: Path = WorkingDirectory,
        env: Map<String, String> = emptyMap(),
        runAfterProcessTermination: (() -> Unit)? = null,
        inputStream: InputStream? = null,
        processor: Processor? = null,
        shellScriptBuilder: ShellScript.() -> Unit,
    ): LoggingProcess = LoggingProcess(
        commandLine = CommandLine(buildShellScriptToTempFile(workingDirectory, shellScriptBuilder).serialized),
        workingDirectory = workingDirectory,
        env = env,
        runAfterProcessTermination = runAfterProcessTermination,
        userProvidedInputStream = inputStream,
        processor = processor,
    )
}
