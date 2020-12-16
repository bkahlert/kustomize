package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.cleanUpOldTempFiles
import com.bkahlert.koodies.concurrent.cleanUpOnShutdown
import com.bkahlert.koodies.nio.file.Paths
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.shell.ShellScript
import com.bkahlert.koodies.shell.ShellScript.Companion.build
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
     * Runs the [command] synchronously in a lightweight fashion and returns if the [substring] is contained in the output.
     */
    fun checkIfOutputContains(command: String, substring: String, caseSensitive: Boolean = false): Boolean = runCatching {
        val flags = if (caseSensitive) "" else "i"
        check(evalShellScript(emptyMap(), Paths.Temp, shellScript = ShellScript { line("$command | grep -q$flags '$substring'") }).waitFor() == 0)
    }.isSuccess

    /**
     * Same as [evalShellScript] but reads `std output` synchronously
     * with neither additional comfort nor additional threads overhead.
     */
    fun evalScriptToOutput(
        environment: Map<String, String> = emptyMap(),
        workingDirectory: Path = Paths.Temp,
        shellScript: ShellScript.() -> Unit,
    ): String = shellScript.build().evalToOutput(environment, workingDirectory)

    fun ShellScript.evalToOutput(
        environment: Map<String, String> = emptyMap(),
        workingDirectory: Path = Paths.Temp,
    ): String = evalShellScript(environment, workingDirectory, this).output

    /**
     * Runs the [shellScriptBuilder] synchronously and returns a [LightweightProcess].
     */
    fun evalShellScript(
        environment: Map<String, String> = emptyMap(),
        workingDirectory: Path = Paths.Temp,
        shellScriptBuilder: ShellScript.() -> Unit,
    ): LightweightProcess = evalShellScript(environment, workingDirectory, shellScriptBuilder.build())

    /**
     * Runs the [shellScript] synchronously and returns a [LightweightProcess].
     */
    fun evalShellScript(
        environment: Map<String, String> = emptyMap(),
        workingDirectory: Path = Paths.Temp,
        shellScript: ShellScript,
    ): LightweightProcess {
        val commandLine = CommandLine(environment, workingDirectory, shellScript.sanitize(workingDirectory).buildTo(tempScriptFile()).cleanUpOnShutdown())
        return commandLine.toLightweightProcess()
    }
}


/**
 * Executes the [shellScript] using the specified [processor] and returns the [Process].
 */
fun executeShellScript(
    workingDirectory: Path = Paths.Temp,
    env: Map<String, String> = emptyMap(),
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript,
    processor: Processor<ManagedProcess> = Processors.noopProcessor(),
): ManagedProcess =
    startShellScript(env, workingDirectory, expectedExitValue, processTerminationCallback, shellScript).process(processor)

/**
 * Executes the [shellScript] without printing to the console and returns the [Process].
 */
fun executeShellScript(
    environment: Map<String, String> = emptyMap(),
    workingDirectory: Path = Paths.Temp,
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = executeShellScript(workingDirectory, environment, expectedExitValue, processTerminationCallback, shellScript.build())

/**
 * Starts the [shellScript] and returns the corresponding [Process].
 */
fun startShellScript(
    environment: Map<String, String> = emptyMap(),
    workingDirectory: Path = Paths.Temp,
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript,
): ManagedProcess =
    CommandLine(environment, workingDirectory, shellScript.sanitize(workingDirectory).buildTo(Processes.tempScriptFile()))
        .toManagedProcess(expectedExitValue, processTerminationCallback)

/**
 * Starts the [shellScript] and returns the corresponding [Process].
 */
fun startShellScript(
    environment: Map<String, String> = emptyMap(),
    workingDirectory: Path = Paths.Temp,
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = startShellScript(environment, workingDirectory, expectedExitValue, processTerminationCallback, shellScript.build())
