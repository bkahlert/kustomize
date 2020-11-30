package com.imgcstmzr.cli

import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.nio.file.writeText
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.get
import strikt.assertions.hasEntry
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
@Isolated("flaky OutputCapture")
class EnvTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should read env file`(logger: InMemoryLogger<*>) {
        val path: Path = tempDir.tempFile("personal", ".env").apply { writeText("A=B\n CC= DD \n") }
        expectThat(Env(logger, path))
            .hasEntry("A", "B")
            .hasEntry("CC", "DD")
    }

    @Test
    fun `should favor environment variables`(logger: InMemoryLogger<*>) {
        val path: Path = tempDir.tempFile("personal", ".env").apply { writeText("JAVA_HOME=dummy\n") }
        expectThat(Env(logger, path))["JAVA_HOME"].isNotEqualTo("dummy")
    }

    @Test
    fun `should still provide access to system env missing env file`(logger: InMemoryLogger<*>) {
        expectThat(Env(logger, Path.of("/nowhere"))).isNotEmpty()
    }
}
