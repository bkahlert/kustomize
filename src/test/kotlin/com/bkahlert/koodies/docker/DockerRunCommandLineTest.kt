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
class DockerRunCommandLineTest {

    companion object {
        val DOCKER_RUN_COMMAND = DockerRunCommandLine(
            dockerRedirects = emptyList(),
            options = DockerRunCommandLine.Options(
                name = "container-name".toContainerName(),
                privileged = true,
                autoCleanup = true,
                interactive = true,
                pseudoTerminal = true,
                mounts = listOf(
                    MountOption(source = Path.of("/a/b"), target = Path.of("/c/d")),
                    MountOption("bind", Path.of("/e/f/../g"), Path.of("//h")),
                )
            ),
            dockerImage = DockerImage.imageWithTag(DockerRepository.of("repo", "name"), Tag("tag")),
            dockerCommand = "work",
            dockerArguments = listOf("-arg1", "--argument", "2", listOf("heredoc 1", "-heredoc-line-2").toHereDoc("HEREDOC").toString())
        )
    }

    @Test
    fun `should build valid docker run`() {
        expectThat(DOCKER_RUN_COMMAND).toStringIsEqualTo("""
            docker \
            run \
            --name \
            container-name \
            --privileged \
            --rm \
            -i \
            -t \
            --mount \
            type=bind,source=/a/b,target=/c/d \
            --mount \
            type=bind,source=/e/f/../g,target=/h \
            repo/name:tag \
            work \
            -arg1 \
            --argument \
            2 \
            <<HEREDOC
            heredoc 1
            -heredoc-line-2
            HEREDOC
            """.trimIndent())
    }
}
