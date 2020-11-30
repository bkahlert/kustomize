package com.bkahlert.koodies.docker

import com.bkahlert.koodies.docker.DockerContainerName.Companion.toContainerName
import com.bkahlert.koodies.shell.toHereDoc
import com.bkahlert.koodies.test.strikt.toStringIsEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import java.nio.file.Path

@Execution(CONCURRENT)
class DockerRunCommandTest {

    companion object {
        val DOCKER_RUN_COMMAND = DockerRunCommand(
            redirects = emptyList(),
            options = DockerRunCommand.Options(
                name = "container-name".toContainerName(),
                privileged = true,
                autoCleanup = true,
                interactive = true,
                pseudoTerminal = true,
                volumes = linkedMapOf(
                    Path.of("/a/b") to Path.of("/c/d"),
                    Path.of("/e/f/../g") to Path.of("//h"),
                )
            ),
            dockerImage = DockerImage.imageWithTag(DockerRepository.of("repo", "name"), Tag("tag")),
            dockerCommand = "work",
            dockerArgs = listOf("-arg1", "--argument 2", listOf("heredoc 1", "-heredoc-line-2").toHereDoc("HEREDOC").toString())
        )
    }

    @Test
    fun `should build valid docker run`() {
        expectThat(DOCKER_RUN_COMMAND).toStringIsEqualTo("""
            docker run \
            --name "container-name" \
            --privileged \
            --rm \
            -i \
            -t \
            --volume /a/b:/c/d \
            --volume /e/g:/h \
            repo/name:tag \
            work \
            -arg1 \
            --argument 2 \
            <<HEREDOC
            heredoc 1
            -heredoc-line-2
            HEREDOC
        """.trimIndent())
    }
}
