package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.cleanUpOldTempFiles
import com.bkahlert.koodies.concurrent.cleanUpOnShutdown
import com.bkahlert.koodies.nio.file.Paths
import com.bkahlert.koodies.nio.file.Paths.WorkingDirectory
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.shell.ShellScript
import com.bkahlert.koodies.shell.ShellScript.Companion.build
import org.codehaus.plexus.util.cli.Commandline
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.time.minutes
import java.lang.Process as JavaProcess

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
    ): JavaProcess {
        val command = shellScript.build().sanitize(workingDirectory).buildTo(tempScriptFile()).cleanUpOnShutdown().serialized
        return Commandline(command).apply {
            addArguments(arguments)
            @Suppress("ExplicitThis")
            this.workingDirectory = workingDirectory.toFile()
            env.forEach { addEnvironment(it.key, it.value) }
        }.execute()
    }

    /**
     * Runs the [command] synchronously in a lightweight fashion and returns if the [substring] is contained in the output.
     */
    fun checkIfOutputContains(command: String, substring: String, caseSensitive: Boolean = false): Boolean = runCatching {
        val flags = if (caseSensitive) "" else "i"
        check(startShellScriptDetached { line("$command | grep -q$flags '$substring'") }.waitFor() == 0)
    }.isSuccess

    /**
     * Same as [evalShellScript] but reads `std output` synchronously
     * with neither additional comfort nor additional threads overhead.
     */
    fun evalScriptToOutput(
        workingDirectory: Path = WorkingDirectory,
        env: Map<String, String> = emptyMap(),
        shellScript: ShellScript.() -> Unit,
    ): String =
        LightweightProcess(CommandLine(command = shellScript.build().sanitize(workingDirectory).buildTo(tempScriptFile()))).output

    /**
     * Runs the [shellScriptBuilder] synchronously and returns the [ManagedProcess].
     */
    fun evalShellScript(
        workingDirectory: Path = WorkingDirectory,
        env: Map<String, String> = emptyMap(),
        shellScriptBuilder: ShellScript.() -> Unit,
    ): LightweightProcess =
        LightweightProcess(CommandLine(command = shellScriptBuilder.build().sanitize(workingDirectory).buildTo(tempScriptFile())))
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
): ManagedProcess {
    return ManagedProcess(
        commandLine = CommandLine(command = shellScript.sanitize(workingDirectory).buildTo(Processes.tempScriptFile())),
        workingDirectory = workingDirectory,
        environment = env,
        expectedExitValue = expectedExitValue,
        processTerminationCallback = processTerminationCallback).process(processor)
}

/**
 * Executes the [shellScript] without printing to the console and returns the [Process].
 */
fun executeShellScript(
    workingDirectory: Path = Paths.Temp,
    env: Map<String, String> = emptyMap(),
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = executeShellScript(workingDirectory, env, expectedExitValue, processTerminationCallback, shellScript.build())

/**
 * Starts the [shellScript] and returns the corresponding [Process].
 */
fun startShellScript(
    workingDirectory: Path = Paths.Temp,
    env: Map<String, String> = emptyMap(),
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript,
): ManagedProcess {
    return ManagedProcess(
        commandLine = CommandLine(command = shellScript.sanitize(workingDirectory).buildTo(Processes.tempScriptFile())),
        workingDirectory = workingDirectory,
        environment = env,
        expectedExitValue = expectedExitValue,
        processTerminationCallback = processTerminationCallback)
}

/**
 * Starts the [shellScript] and returns the corresponding [Process].
 */
fun startShellScript(
    workingDirectory: Path = Paths.Temp,
    env: Map<String, String> = emptyMap(),
    expectedExitValue: Int = 0,
    processTerminationCallback: (() -> Unit)? = null,
    shellScript: ShellScript.() -> Unit,
): ManagedProcess = startShellScript(workingDirectory, env, expectedExitValue, processTerminationCallback, shellScript.build())
