package com.bkahlert.koodies.docker

import com.bkahlert.koodies.concurrent.process.DockerBuilder
import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.Processes.checkIfOutputContains
import com.bkahlert.koodies.concurrent.process.Processes.startShellScript
import com.bkahlert.koodies.concurrent.process.Processes.startShellScriptDetached
import com.bkahlert.koodies.concurrent.process.RunningProcess
import com.bkahlert.koodies.regex.RegexBuilder
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.time.sleep
import com.imgcstmzr.util.Paths
import org.codehaus.plexus.util.cli.Commandline
import java.nio.file.Path
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

/**
 * Provides methods to create and interact with a [DockerProcess].
 */
@OptIn(ExperimentalTime::class)
object Docker {
    /**
     * Whether the Docker engine itself is running.
     */
    val isEngineRunning: Boolean get() = !checkIfOutputContains("docker info", "error")

    /**
     * Whether a Docker container with the given [name] is running.
     */
    fun isContainerRunning(name: String): Boolean =
        checkIfOutputContains("""docker ps --no-trunc --format "{{.Names}}" --filter "name=^$name${'$'}"""", name)

    /**
     * Whether a Docker container—no matter if it's running or not—exists.
     */
    fun exists(name: String): Boolean =
        checkIfOutputContains("""docker ps --no-trunc --format "{{.Names}}" --filter "name=^$name${'$'} --all"""", name)

    fun run(
        workingDirectory: Path = Paths.WORKING_DIRECTORY,
        outputProcessor: (DockerProcess.(IO) -> Unit)? = null,
        init: DockerBuilder.() -> Unit,
    ): DockerProcess {
        var runningProcess: RunningProcess = RunningProcess.nullRunningProcess
        val name = extractName(init)
        return DockerProcess(name) { runningProcess }.also { dockerProcess ->
            runningProcess = startShellScript(
                workingDirectory = workingDirectory,
                shellScript = { docker(init) },
                outputProcessor = outputProcessor?.let { { output -> it(dockerProcess, output) } },
                runAfterProcessTermination = {
                    stop(name)
                    remove(name, forcibly = true)
                },
            )
        }
    }

    /**
     * Explicitly stops the Docker container with the given [name] **asynchronously**.
     */
    fun stop(name: String) {
        startShellScriptDetached { !"docker stop \"$name\"" }
        1.seconds.sleep()
    }

    /**
     * Explicitly (stops and) removes the Docker container with the given [name] **synchronously**.
     *
     * If needed even [forcibly].
     */
    fun remove(name: String, forcibly: Boolean = false) {
        val forceOption = if (forcibly) " --force" else ""
        startShellScript { !"docker rm$forceOption \"$name\"" }.waitForCompletion()
        1.seconds.sleep()
    }

    /**
     * A [Regex] that matches valid Docker container names.
     */
    private val containerNameRegex: Regex = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{7,}")

    /**
     * Extracts the name of a Docker command
     */
    fun extractName(init: DockerBuilder.() -> Unit): String =
        mutableListOf<String>()
            .also { DockerBuilder(it).apply(init) }
            .map { cmd -> Commandline(cmd).arguments.dropWhile { arg -> !arg.contains("--name") } }
            .filter { argList -> argList.size > 1 }
            .mapNotNull { argList -> if (argList.size > 1) argList.drop(1).first() else null }
            .single { it.matches(containerNameRegex) }

    /**
     * Transforms this [String] to a valid Docker container name.
     */
    fun String.toContainerName(): String {
        var replaceWithXToGuaranteeAValidName = true
        return map { c ->
            val isAlphaNumeric = RegexBuilder.alphanumericCharacters.contains(c)
            if (isAlphaNumeric) replaceWithXToGuaranteeAValidName = false
            if (replaceWithXToGuaranteeAValidName) return@map "X"
            when {
                isAlphaNumeric -> c
                "._-".contains(c) -> c
                c.isWhitespace() -> "-"
                else -> '_'
            }
        }.joinToString("",
            postfix = (8 - length)
                .takeIf { it > 0 }?.let {
                    String.random(it, String.random.alphanumericCharacters)
                } ?: "")
            .also { check(it.matches(containerNameRegex)) }
    }
}

