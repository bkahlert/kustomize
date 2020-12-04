package com.bkahlert.koodies.docker

import com.bkahlert.koodies.docker.DockerRunCommandLineBuilder.Companion.buildRunCommand
import com.bkahlert.koodies.docker.DockerRunCommandLineTest.Companion.DOCKER_RUN_COMMAND
import com.bkahlert.koodies.shell.HereDocBuilder.hereDoc
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class DockerRunCommandLineBuilderTest {

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
}
