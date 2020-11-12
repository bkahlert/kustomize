package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.shell.toHereDoc
import com.imgcstmzr.patch.isEqualTo
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.hasContent
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isExecutable
import java.nio.file.Path

@Execution(CONCURRENT)
class ShellScriptTest {

    private val tempDir = tempDir().deleteOnExit()

    private fun shellScript() = ShellScript().apply {
        shebang()
        changeDirectoryOrExit(Path.of("/some/where"))
        !"""
                echo "Hello World!"
                echo "Bye!"
            """.trimIndent()
        exit(42)
    }

    @Test
    fun `should build valid script`() {
        expectThat(shellScript().build()).isEqualTo("""
            #!/bin/sh
            cd "/some/where" || exit 1
            echo "Hello World!"
            echo "Bye!"
            exit 42
            
        """.trimIndent())
    }

    @Test
    fun `should write valid script`() {
        val file = tempDir.tempFile(extension = ".sh").deleteOnExit()
        shellScript().buildTo(file)
        expectThat(file).hasContent("""
            #!/bin/sh
            cd "/some/where" || exit 1
            echo "Hello World!"
            echo "Bye!"
            exit 42
            
        """.trimIndent())
    }

    @Test
    fun `should write executable script`() {
        val file = tempDir.tempFile(extension = ".sh").deleteOnExit()
        val returnedScript = shellScript().buildTo(file)
        expectThat(returnedScript).isExecutable()
    }

    @Test
    fun `should return same file as saved to file`() {
        val file = tempDir.tempFile(extension = ".sh").deleteOnExit()
        val returnedScript = shellScript().buildTo(file)
        expectThat(returnedScript).isEqualTo(file)
    }

    @Nested
    inner class DockerCommand {
        @Test
        fun `should build valid docker run`() {
            expectThat(ShellScript().apply {
                shebang()
                docker {
                    run(
                        name = "container-name",
                        volumes = mapOf(
                            Path.of("/a/b") to Path.of("/c/d"),
                            Path.of("/e/f/../g") to Path.of("//h")
                        ),
                        image = "image/name",
                        args = listOf("-arg1", "--argument 2", listOf("heredoc 1", "-heredoc-line-2").toHereDoc("HEREDOC")),
                    )
                }
            }.build()).isEqualTo("""
            #!/bin/sh
            docker run \
            --name "container-name" \
            --rm \
            -i \
            --volume /a/b:/c/d \
            --volume /e/g:/h \
            image/name \
            -arg1 \
            --argument 2 \
            <<HEREDOC
            heredoc 1
            -heredoc-line-2
            HEREDOC

        """.trimIndent())
        }

        @Test
        fun `should build allow redirection`() {
            expectThat(ShellScript().apply {
                shebang()
                docker {
                    run(
                        redirectStdErrToStdOut = true,
                        name = "container-name",
                        image = "image/name",
                    )
                }
            }.build()).isEqualTo("""
            #!/bin/sh
            2>&1 docker run \
            --name "container-name" \
            --rm \
            -i \
            image/name

        """.trimIndent())
        }
    }
}
