package com.bkahlert.koodies.docker

import com.bkahlert.koodies.builder.ListBuilder
import com.bkahlert.koodies.builder.ListBuilderInit
import com.bkahlert.koodies.builder.build
import com.bkahlert.koodies.builder.buildTo
import com.bkahlert.koodies.concurrent.process.CommandLine
import com.bkahlert.koodies.docker.DockerRunCommandLine.Options
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.toPath
import java.nio.file.Path

data class DockerRunCommandLine(
    val dockerRedirects: List<String> = emptyList(),
    val options: Options = Options(),
    val dockerImage: DockerImage,
    val dockerCommand: String? = null,
    val dockerArguments: List<String> = emptyList(),
) : CommandLine(dockerRedirects, "docker", mutableListOf("run").apply {
    addAll(options)
    add(dockerImage.formatted)
    dockerCommand?.also { add(it) }
    addAll(dockerArguments)
}) {
    data class Options(
        val entryPoint: String? = null,
        val name: DockerContainerName? = null,
        val privileged: Boolean = false,
        val autoCleanup: Boolean = true,
        val interactive: Boolean = true,
        val pseudoTerminal: Boolean = false,
        val mounts: List<MountOption> = emptyList(),
    ) : List<String> by ListBuilder.build({
        entryPoint?.also { +"--entrypoint" + entryPoint }
        name?.also { +"--name" + name.sanitized }
        privileged.takeIf { it }?.also { +"--privileged" }
        autoCleanup.takeIf { it }?.also { +"--rm" }
        interactive.takeIf { it }?.also { +"-i" }
        pseudoTerminal.takeIf { it }?.also { +"-t" }
        mounts.forEach { +it }
    })

    override fun toString(): String = super.toString()
}

@DockerCommandDsl
abstract class DockerRunCommandLineBuilder {

    class ImageProvidedBuilder(private val image: DockerImage) {
        infix fun run(init: DockerRunCommandLineBuilder.() -> Unit): DockerRunCommandLine = build(image, init)
    }

    companion object {
        fun build(init: DockerImageBuilder.() -> Any): ImageProvidedBuilder =
            ImageProvidedBuilder(DockerImageBuilder.build(init))

        fun DockerImage.buildRunCommand(init: DockerRunCommandLineBuilder.() -> Unit): DockerRunCommandLine = build(this, init)

        fun build(dockerImage: DockerImage, init: DockerRunCommandLineBuilder.() -> Unit): DockerRunCommandLine {
            val dockerRedirects = mutableListOf<String>()
            var dockerOptions = Options()
            var dockerCommand: String? = null
            val dockerArguments = mutableListOf<String>()
            object : DockerRunCommandLineBuilder() {
                override val redirects
                    get() = dockerRedirects
                override var dockerOptions
                    get() = dockerOptions
                    set(value) = value.run { dockerOptions = this }
                override var dockerCommand
                    get() = dockerCommand
                    set(value) = value.run { dockerCommand = this }
                override val dockerArguments
                    get() = dockerArguments
            }.apply(init)
            return DockerRunCommandLine(
                dockerRedirects = dockerRedirects,
                options = dockerOptions,
                dockerImage = dockerImage,
                dockerCommand = dockerCommand,
                dockerArguments = dockerArguments,
            )
        }
    }

    protected abstract val redirects: MutableList<String>
    protected abstract var dockerOptions: Options
    protected abstract var dockerCommand: String?
    protected abstract val dockerArguments: MutableList<String>

    fun redirects(init: ListBuilderInit<String>) = init.build()
    fun options(init: OptionsBuilder.() -> Unit) = OptionsBuilder.build(init).run { dockerOptions = this }
    fun command(init: () -> String?) = init.build()?.run { dockerCommand = this }
    fun arguments(init: ListBuilderInit<String>) = init.buildTo(dockerArguments)
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

    fun entrypoint(entryPoint: () -> String?) = options.copy(entryPoint = entryPoint()).run { options = this }
    fun name(name: () -> String?) = options.copy(name = name()?.let { DockerContainerName(it) }).run { options = this }
    fun containerName(name: () -> DockerContainerName?) = options.copy(name = name()).run { options = this }
    fun privileged(privileged: () -> Boolean) = options.copy(privileged = privileged()).run { options = this }
    fun autoCleanup(autoCleanup: () -> Boolean) = options.copy(autoCleanup = autoCleanup()).run { options = this }
    fun interactive(interactive: () -> Boolean) = options.copy(interactive = interactive()).run { options = this }
    fun pseudoTerminal(pseudoTerminal: () -> Boolean) = options.copy(pseudoTerminal = pseudoTerminal()).run { options = this }
    fun mounts(init: ListBuilderInit<MountOption>) = init.build().also { options.copy(mounts = options.mounts + it).run { options = this } }

    infix fun Path.mountAt(target: String) {
        mounts {
            +MountOption(source = this@mountAt, target = target.toPath())
        }
    }

    override fun toString(): String = options.toString()
}

data class MountOption(val type: String = "bind", val source: Path, val target: Path) :
    List<String> by listOf("--mount", "type=$type,source=${source.serialized},target=${target.serialized}")
