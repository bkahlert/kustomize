package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.cleanUpOldTempFiles
import com.bkahlert.koodies.concurrent.cleanUpOnShutdown
import com.bkahlert.koodies.concurrent.process.Processors.noopProcessor
import com.bkahlert.koodies.nio.file.Paths
import com.bkahlert.koodies.nio.file.Paths.WorkingDirectory
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.shell.ShellScript
import com.bkahlert.koodies.shell.ShellScript.Companion.build
import com.bkahlert.koodies.shell.shebang
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import org.codehaus.plexus.util.cli.Commandline
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
    @Deprecated("Use start shell script (or new start as shell script)")
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
    @Deprecated("Use start shell script (or new start as shell script)")
    fun cheapEvalShellScript(
        workingDirectory: Path = WorkingDirectory,
        env: Map<String, String> = emptyMap(),
        shellScript: ShellScript.() -> Unit,
    ): String =
        startShellScript(workingDirectory = workingDirectory, env = env, shellScript = shellScript)
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
        shellScriptBuilder: ShellScript.() -> Unit,
    ): LoggedProcess {
        return executeShellScript(
            workingDirectory = workingDirectory,
            env = env,
            shellScript = shellScriptBuilder.build(),
            processor = noopProcessor()
        ).loggedProcess.join().also { println("$it") }
    }

    /**
     * Executes the [shellScript] using the specified [processor] and returns the [LoggingProcess].
     */
    fun executeShellScript(
        workingDirectory: Path = Paths.Temp,
        env: Map<String, String> = emptyMap(),
        expectedExitValue: Int = 0,
        processTerminationCallback: (() -> Unit)? = null,
        shellScript: ShellScript,
        processor: Processor<DelegatingProcess> = noopProcessor(),
    ): LoggingProcess {
        val scriptFile = shellScript.buildTo(tempScriptFile())
        return CommandLine(scriptFile).execute(workingDirectory, env, expectedExitValue, processTerminationCallback, processor = processor)
    }

    /**
     * Executes the [shellScript] without printing to the console and returns the [LoggingProcess].
     */
    fun executeShellScript(
        workingDirectory: Path = Paths.Temp,
        env: Map<String, String> = emptyMap(),
        expectedExitValue: Int = 0,
        processTerminationCallback: (() -> Unit)? = null,
        shellScript: ShellScript.() -> Unit,
    ): LoggingProcess = executeShellScript(workingDirectory, env, expectedExitValue, processTerminationCallback, shellScript.build())

    /**
     * Starts the [shellScript] and returns the corresponding [DelegatingProcess].
     */
    fun startShellScript(
        workingDirectory: Path = Paths.Temp,
        env: Map<String, String> = emptyMap(),
        expectedExitValue: Int = 0,
        processTerminationCallback: (() -> Unit)? = null,
        shellScript: ShellScript,
    ): DelegatingProcess {
        val scriptFile = shellScript.buildTo(tempScriptFile())
        return CommandLine(scriptFile).lazyStart(workingDirectory, env, expectedExitValue, processTerminationCallback)
    }

    /**
     * Starts the [shellScript] and returns the corresponding [DelegatingProcess].
     */
    fun startShellScript(
        workingDirectory: Path = Paths.Temp,
        env: Map<String, String> = emptyMap(),
        expectedExitValue: Int = 0,
        processTerminationCallback: (() -> Unit)? = null,
        shellScript: ShellScript.() -> Unit,
    ): DelegatingProcess = startShellScript(workingDirectory, env, expectedExitValue, processTerminationCallback, shellScript.build())

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
}
