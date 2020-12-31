package com.imgcstmzr.cli

import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.withTempDir
import koodies.io.path.randomFile
import koodies.io.path.writeText
import koodies.logging.InMemoryLogger
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

    @Test
    fun InMemoryLogger.`should read env file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path: Path = randomFile("personal", ".env").apply { writeText("A=B\n CC= DD \n") }
        expectThat(Env(this@`should read env file`, path))
            .hasEntry("A", "B")
            .hasEntry("CC", "DD")
    }

    @Test
    fun InMemoryLogger.`should favor environment variables`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path: Path = randomFile("personal", ".env").apply { writeText("JAVA_HOME=dummy\n") }
        expectThat(Env(this@`should favor environment variables`, path))["JAVA_HOME"].isNotEqualTo("dummy")
    }

    @Test
    fun InMemoryLogger.`should still provide access to system env missing env file`() {
        expectThat(Env(this, Path.of("/nowhere"))).isNotEmpty()
    }
}
