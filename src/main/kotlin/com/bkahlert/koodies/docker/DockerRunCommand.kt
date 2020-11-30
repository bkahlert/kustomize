package com.bkahlert.koodies.docker

import com.bkahlert.koodies.builder.ListBuilder
import com.bkahlert.koodies.builder.MapBuilder
import com.bkahlert.koodies.concurrent.process.Command
import com.bkahlert.koodies.docker.DockerRunCommand.Options
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.string.quoted
import java.nio.file.Path

data class DockerRunCommand(
    override val redirects: List<String> = emptyList(),
    val options: Options = Options(),
    val dockerImage: DockerImage,
    val dockerCommand: String? = null,
    val dockerArgs: List<String> = emptyList(),
) : Command(redirects, "docker run", mutableListOf<String>().apply {
    addAll(options)
    add(dockerImage.formatted)
    dockerCommand?.also { add(it) }
    addAll(dockerArgs)
}) {
    data class Options(
        val name: DockerContainerName? = null,
        val privileged: Boolean = false,
        val autoCleanup: Boolean = true,
        val interactive: Boolean = true,
        val pseudoTerminal: Boolean = false,
        val volumes: Map<Path, Path> = emptyMap(),
    ) : List<String> by listOf(
        (name != null) to "--name ${name?.sanitized.quoted}",
        privileged to "--privileged",
        autoCleanup to "--rm",
        interactive to "-i",
        pseudoTerminal to "-t",
        *volumes.map { (from, to) -> true to "--volume ${from.toAbsolutePath().normalize().serialized}:$to" }.toTypedArray(),
    ).mapNotNull(transform = { (active, option) -> if (active) option else null })

    override fun toString(): String = super.toString()
}

@DockerCommandDsl
abstract class DockerRunCommandBuilder {

    class ImageProvidedBuilder(private val image: DockerImage) {
        infix fun run(init: DockerRunCommandBuilder.() -> Unit): DockerRunCommand = build(image, init)
    }

    companion object {
        fun build(init: DockerImageBuilder.() -> Any): ImageProvidedBuilder =
            ImageProvidedBuilder(DockerImageBuilder.build(init))

        fun DockerImage.buildRunCommand(init: DockerRunCommandBuilder.() -> Unit): DockerRunCommand =
            build(this, init)

        fun build(dockerImage: DockerImage, init: DockerRunCommandBuilder.() -> Unit): DockerRunCommand {
            var redirects = emptyList<String>()
            var dockerOptions = Options()
            var dockerCommand: String? = null
            var dockerArgs = emptyList<String>()
            object : DockerRunCommandBuilder() {
                override var redirects
                    get() = redirects
                    set(value) = value.run { redirects = this }
                override var dockerOptions
                    get() = dockerOptions
                    set(value) = value.run { dockerOptions = this }
                override var dockerCommand
                    get() = dockerCommand
                    set(value) = value.run { dockerCommand = this }
                override var dockerArgs
                    get() = dockerArgs
                    set(value) = value.run { dockerArgs = this }
            }.apply(init)
            return DockerRunCommand(redirects = redirects,
                options = dockerOptions,
                dockerImage = dockerImage,
                dockerCommand = dockerCommand,
                dockerArgs = dockerArgs)
        }
    }

    protected abstract var redirects: List<String>
    protected abstract var dockerOptions: Options
    protected abstract var dockerCommand: String?
    protected abstract var dockerArgs: List<String>

    fun redirects(init: ListBuilder<String>.() -> Unit) = ListBuilder.build(init).run { redirects = this }
    fun options(init: OptionsBuilder.() -> Unit) = OptionsBuilder.build(init).run { dockerOptions = this }
    fun command(command: String?) = command.run { dockerCommand = this }
    fun args(args: Iterable<CharSequence>): Unit = args.run { map { "$it" }.run { dockerArgs = this } }
    fun args(vararg args: CharSequence) = args(args.map { "$it" })
    fun args(init: ListBuilder<in CharSequence>.() -> Unit) = ListBuilder.build(init) { "$it" }.run { dockerArgs = this }
}

@DockerCommandDsl
abstract class OptionsBuilder {
    companion object {
        inline fun build(init: OptionsBuilder.() -> Unit): Options {
            var options = Options()
            object : OptionsBuilder() {
                override var options: Options
                    get() = options
                    set(value) = value.run { options = this }
            }.apply(init)
            return options
        }
    }

    protected abstract var options: Options
    fun name(name: () -> String?) = options.copy(name = name()?.let { DockerContainerName(it) }).run { options = this }
    fun containerName(name: () -> DockerContainerName?) = options.copy(name = name()).run { options = this }
    fun privileged(privileged: () -> Boolean) = options.copy(privileged = privileged()).run { options = this }
    fun autoCleanup(autoCleanup: () -> Boolean) = options.copy(autoCleanup = autoCleanup()).run { options = this }
    fun interactive(interactive: () -> Boolean) = options.copy(interactive = interactive()).run { options = this }
    fun pseudoTerminal(pseudoTerminal: () -> Boolean) = options.copy(pseudoTerminal = pseudoTerminal()).run { options = this }
    fun volumes(init: MapBuilder<Path, Path>.() -> Unit) = options.copy(volumes = MapBuilder.build(init)).run { options = this }
}

