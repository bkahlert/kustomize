package com.bkahlert.koodies.shell

import com.bkahlert.koodies.docker.docker
import com.bkahlert.koodies.nio.file.Paths
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.shell.HereDocBuilder.hereDoc
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.bkahlert.koodies.test.strikt.toStringIsEqualTo
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.hasContent
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import strikt.assertions.isExecutable
import java.nio.file.Path

@Execution(CONCURRENT)
class ShellScriptTest {

    private val tempDir = tempDir().deleteOnExit()

    private fun shellScript() = ShellScript().apply {
        shebang
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
            cd "/some/where" || exit -1
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
            cd "/some/where" || exit -1
            echo "Hello World!"
            echo "Bye!"
            exit 42
            
        """.trimIndent())
    }

    @Test
    fun `should sanitize script`() {
        val sanitized = ShellScript(content = """
              
              
            cd "/some/where" 
            echo "Hello World!"
            
            #!/bin/sh
            echo "Bye!"
            exit 42
        """.trimIndent()).sanitize(Paths.Temp)

        expectThat(sanitized.build()).matchesCurlyPattern("""
            #!/bin/sh
            cd "{}" || exit -1
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
            expectThat(ShellScript {
                `#!`
                docker { "image" / "name" } run {
                    options {
                        name { "container-name" }
                        mounts {
                            Path.of("/a/b") mountAt "/c/d"
                            Path.of("/e/f/../g") mountAt "//h"
                        }
                    }
                    arguments {
                        +"-arg1"
                        +"--argument" + "2"
                        +hereDoc(label = "HEREDOC") {
                            +"heredoc 1"
                            +"-heredoc-line-2"
                        }
                    }
                }
            }.build()).isEqualTo("""
            #!/bin/sh
            docker \
            run \
            --name \
            container-name \
            --rm \
            -i \
            --mount \
            type=bind,source=/a/b,target=/c/d \
            --mount \
            type=bind,source=/e/f/../g,target=/h \
            image/name \
            -arg1 \
            --argument \
            2 \
            <<HEREDOC
            heredoc 1
            -heredoc-line-2
            HEREDOC

        """.trimIndent())
        }

        @Test
        fun `should build allow redirection`() {
            expectThat(ShellScript().apply {
                shebang
                docker { "image" / "name" } run {
                    redirects { +"2>&1" }
                    options { name { "container-name" } }
                }
            }.build()).isEqualTo("""
            #!/bin/sh
            docker \
            run \
            --name \
            container-name \
            --rm \
            -i \
            image/name

        """.trimIndent())
        }
    }

    @Test
    fun `should have an optional name`() {
        val sh = ShellScript("test") { !"exit 0" }
        expectThat(sh).toStringIsEqualTo("Script(name=test;content=exit 0})")
    }

    @Test
    fun `should build comments`() {
        val sh = ShellScript {
            comment("test")
            !"exit 0"
        }
        expectThat(sh.lines).containsExactly("# test", "exit 0")
    }

    @Test
    fun `should build multi-line comments`() {
        expectThat(ShellScript {

            comment("""
                line 1
                line 2
            """.trimIndent())
            !"exit 0"

        }.lines).containsExactly("# line 1", "# line 2", "exit 0")
    }
}

val Assertion.Builder<ShellScript>.built
    get() = get("built shell script %s") { build() }
