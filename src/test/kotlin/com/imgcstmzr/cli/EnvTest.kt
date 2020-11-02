package com.imgcstmzr.cli

import com.imgcstmzr.util.writeText
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.get
import strikt.assertions.hasEntry
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotEqualTo
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
@Isolated // flaky OutputCapture
class EnvTest {

    @Test
    fun `should read env file`() {
        val path: Path = createTempFile("personal", ".env").toPath().also { it.writeText("A=B\n CC= DD \n") }
        expectThat(Env(path))
            .hasEntry("A", "B")
            .hasEntry("CC", "DD")
    }

    @Test
    fun `should favor environment variables`() {
        val path: Path = createTempFile("personal", ".env").toPath().also { it.writeText("JAVA_HOME=dummy\n") }
        expectThat(Env(path))["JAVA_HOME"].isNotEqualTo("dummy")
    }

    @Test
    fun `should still provide access to system env missing env file`() {
        expectThat(Env(Path.of("/nowhere"))).isNotEmpty()
    }
}
