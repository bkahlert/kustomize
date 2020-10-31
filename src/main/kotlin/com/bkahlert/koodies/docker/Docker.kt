package com.bkahlert.koodies.docker

import com.bkahlert.koodies.concurrent.process.Exec
import com.bkahlert.koodies.concurrent.process.Exec.Async.startShellScript
import com.bkahlert.koodies.concurrent.process.Exec.Sync.checkIfOutputContains
import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.concurrent.process.RunningProcess
import com.bkahlert.koodies.regex.RegexBuilder
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.time.sleep
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
        Exec.Sync.evalShellScript { !"""docker ps --no-trunc --filter "name=$name"""" }.output.lines().drop(1).size == 1

    /**
     * Whether a Docker container—no matter if it's running or not—exists.
     */
    fun exists(name: String): Boolean =
        Exec.Sync.evalShellScript { !"""docker ps --no-trunc --filter "name=$name" --all""" }.output.lines().drop(1).size == 1

    @Suppress("SpellCheckingInspection")
    fun run(
        name: String,
        volumes: Map<Path, Path> = emptyMap(),
        image: String,
        args: List<String> = emptyList(),
        outputProcessor: (DockerProcess.(IO) -> Unit)? = null,
    ): DockerProcess {
        val containerName = name.toContainerName()
        var runningProcess: RunningProcess = RunningProcess.nullRunningProcess
        return DockerProcess(containerName) { runningProcess }.also { dockerProcess ->
            runningProcess = startShellScript(
                outputProcessor = outputProcessor?.let { { output -> it(dockerProcess, output) } },
                runAfterProcessTermination = {
                    stop(name)
                    remove(name, forcibly = true)
                },
            ) {
                !"""
                    docker run \
                      --name "$containerName" \
                      --rm \
                      -i \
                      ${volumes.map { volume -> "--volume ${volume.key}:${volume.value}" }.joinToString(" ")} \
                      $image \
                      ${args.joinToString(" ")}
                """.trimIndent()
            }
        }
    }

    /**
     * Explicitly stops the Docker container with the given [name].
     */
    fun stop(name: String) {
        startShellScript { !"docker stop \"$name\"" }.waitForCompletion()
        1.seconds.sleep()
    }

    /**
     * Explicitly (stops and) removes the Docker container with the given [name].
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

