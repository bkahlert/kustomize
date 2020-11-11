package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.docker.Docker.toContainerName
import com.bkahlert.koodies.nio.file.conditioned
import com.bkahlert.koodies.string.LineSeparators
import com.imgcstmzr.util.makeExecutable
import com.imgcstmzr.util.quoted
import com.imgcstmzr.util.writeText
import java.nio.file.Path

@DslMarker
annotation class ShellScriptMarker

@ShellScriptMarker
class ShellScript {


    val lines: MutableList<String> = mutableListOf()
    fun shebang(interpreter: Path = Path.of("/bin/sh")) {
        lines.add("#!${interpreter.conditioned}")
    }

    fun changeDirectoryOrExit(directory: Path, @Suppress("UNUSED_PARAMETER") errorCode: Int = 1) {
        lines.add("cd \"$directory\" || exit 1")
    }

    operator fun String.not() {
        lines.add(this)
    }

    /**
     * Builds a script line based on [words]. All words are combined using a single space.
     */
    fun line(vararg words: String) {
        lines.add(words.joinToString(" "))
    }

    /**
     * Builds a script [line] based on a single string already making up that script.
     */
    fun line(line: String) {
        lines.add(line)
    }

    /**
     * Builds a [command] call using the [arguments].
     */
    fun command(command: String, vararg arguments: String) {
        lines.add(listOf(command, *arguments).joinToString(" "))
    }

    fun docker(init: DockerBuilder.() -> Unit) = DockerBuilder(lines).apply(init)

    fun exit(code: Int) {
        lines.add("exit $code")
    }

    fun build(): String = lines.joinToString(LineSeparators.LF, postfix = LineSeparators.LF)
    fun buildTo(path: Path): Path = build().let { script ->
        path.apply {
            writeText(script)
            makeExecutable()
        }
    }
}

@ShellScriptMarker
class DockerBuilder(private val lines: MutableList<String>) {
    fun run(
        redirectStdErrToStdOut: Boolean = false,
        name: String,
        privileged: Boolean = false,
        autoCleanup: Boolean = true,
        interactive: Boolean = true,
        pseudoTerminal: Boolean = false,
        volumes: Map<Path, Path> = emptyMap(),
        image: String,
        args: List<String> = emptyList(),
    ) {
        val containerName = name.toContainerName()
        val continuation = "\\${LineSeparators.LF}"

        val redirections = mutableListOf<String>()
        if (redirectStdErrToStdOut) redirections.add("2>&1")

        lines.add(listOf(
            *redirections.toTypedArray(),
            "docker",
            "run",
            continuation + "--name ${containerName.quoted}",
            *listOf(
                privileged to "--privileged",
                autoCleanup to "--rm",
                interactive to "-i",
                pseudoTerminal to "-t",
            ).filter { (option, _) -> option }.map { (_, flag) -> continuation + flag }.toTypedArray(),
            *volumes.map { volume -> continuation + "--volume ${volume.key.conditioned}:${volume.value}" }.toTypedArray(),
            continuation + image,
            *args.map { continuation + it }.toTypedArray(),
        ).joinToString(" "))
    }
}
