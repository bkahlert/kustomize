package com.bkahlert.koodies.docker

import com.bkahlert.koodies.docker.DockerRunCommandLineBuilder.Companion.buildRunCommand
import com.bkahlert.koodies.docker.DockerRunCommandLineTest.Companion.DOCKER_RUN_COMMAND
import com.bkahlert.koodies.shell.HereDocBuilder.hereDoc
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class DockerRunCommandLineBuilderTest {

    private val dockerImage = DockerImageBuilder.build { "repo" / "name" tag "tag" }

    @Test
    fun `should build valid docker run`() {
        val dockerRunCommand = dockerImage.buildRunCommand {
            redirects {}
            options {
                name { "container-name" }
                privileged { true }
                autoCleanup { true }
                interactive { true }
                pseudoTerminal { true }
                mounts {
                    Path.of("/a/b") mountAt "/c/d"
                    +MountOption("bind", Path.of("/e/f/../g"), Path.of("//h"))
                }
            }
            command { "work" }
            arguments {
                +"-arg1"
                +"--argument" + "2"
                +hereDoc(label = "HEREDOC") {
                    +"heredoc 1"
                    +"-heredoc-line-2"
                }
            }
        }
        expectThat(dockerRunCommand).isEqualTo(DOCKER_RUN_COMMAND)
    }

    @Test
    fun `should build same format for no sub builders and empty sub builders`() {
        val commandBuiltWithNoBuilders = dockerImage.buildRunCommand { }
        val commandBuiltWithEmptyBuilders = dockerImage.buildRunCommand {
            options { }
            arguments { }
        }
        expectThat(commandBuiltWithNoBuilders).isEqualTo(commandBuiltWithEmptyBuilders)
    }

    @Test
    fun `should set auto cleanup as default`() {
        expectThat(dockerImage.buildRunCommand {}.options).contains("--rm")
    }

    @Test
    fun `should set interactive as default`() {
        expectThat(dockerImage.buildRunCommand {}.options).contains("-i")
    }
}
