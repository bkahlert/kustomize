package koodies.ext.docker

import koodies.builder.build
import koodies.concurrent.process.CommandLine
import koodies.concurrent.process.ManagedProcess
import koodies.concurrent.process.Processor
import koodies.concurrent.process.process
import koodies.concurrent.process.silentlyProcess
import koodies.docker.DockerCommandLineBuilder
import koodies.docker.DockerCommandLineOptions
import koodies.docker.DockerCommandLineOptionsBuilder
import koodies.docker.DockerImageBuilder
import koodies.docker.buildCommandLine
import java.nio.file.Path

fun Path.docker(
    imageBuilder: DockerImageBuilder.() -> Any,
    commandLineOptionsBuilder: DockerCommandLineOptionsBuilder.() -> Unit,
    vararg arguments: String,
): Int = DockerImageBuilder.build(imageBuilder).buildCommandLine {
    options(DockerCommandLineOptionsBuilder.build(commandLineOptionsBuilder))
    commandLine(CommandLine(emptyMap(), this@docker, "", *arguments))
}.toManagedProcess().silentlyProcess().waitForTermination()

fun Path.docker(
    imageBuilder: DockerImageBuilder.() -> Any,
    commandLineOptionsBuilder: DockerCommandLineOptionsBuilder.() -> Unit,
    vararg arguments: String,
    processor: Processor<ManagedProcess> = {},
): Int = DockerImageBuilder.build(imageBuilder).buildCommandLine {
    val options: DockerCommandLineOptions = commandLineOptionsBuilder.build()
    options(options)
    commandLine(CommandLine(emptyMap(), this@docker, "", *arguments))
}.toManagedProcess().process(processor).waitForTermination()

fun docker(imageBuilder: DockerImageBuilder.() -> Any, commandLineBuilder: DockerCommandLineBuilder.() -> Unit, processor: Processor<ManagedProcess> = {}) {
    DockerImageBuilder.build(imageBuilder).buildCommandLine(commandLineBuilder).toManagedProcess().process(processor)
}

fun docker(imageBuilder: DockerImageBuilder.() -> Any, commandLineBuilder: DockerCommandLineBuilder.() -> Unit) =
    docker(imageBuilder, commandLineBuilder, {})
