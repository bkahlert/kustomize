package com.bkahlert.koodies.docker

import com.bkahlert.koodies.docker.DockerRunCommandBuilder.Companion.buildRunCommand
import com.bkahlert.koodies.docker.DockerRunCommandTest.Companion.DOCKER_RUN_COMMAND
import com.bkahlert.koodies.shell.HereDoc
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class DockerRunCommandBuilderTest {

    @Test
    fun `should build valid docker run`() {
        val dockerRunCommand = DockerImageBuilder.build { "repo" / "name" tag "tag" }.buildRunCommand {
            redirects {}
            options {
                name { "container-name" }
                privileged { true }
                autoCleanup { true }
                interactive { true }
                pseudoTerminal { true }
                volumes {
                    Path.of("/a/b") to Path.of("/c/d")
                    Path.of("/e/f/../g") to Path.of("//h")
                }
            }
            command("work")
            args {
                +"-arg1"
                +"--argument 2"
                +HereDoc("heredoc 1", "-heredoc-line-2", label = "HEREDOC")
            }
        }
        expectThat(dockerRunCommand).isEqualTo(DOCKER_RUN_COMMAND)
    }
}
