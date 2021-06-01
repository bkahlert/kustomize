package com.imgcstmzr.cli

import koodies.io.path.randomFile
import koodies.io.path.writeText
import koodies.logging.InMemoryLogger
import koodies.test.UniqueId
import koodies.test.withTempDir
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.get
import strikt.assertions.hasEntry
import strikt.assertions.isNotEmpty
import strikt.assertions.isNotEqualTo
import java.nio.file.Path

class EnvTest {

    @Test
    fun `should read env file`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
        val path: Path = randomFile("personal", ".env").apply { writeText("A=B\n CC= DD \n") }
        expectThat(Env(logger, path))
            .hasEntry("A", "B")
            .hasEntry("CC", "DD")
    }

    @Test
    fun `should favor environment variables`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
        val path: Path = randomFile("personal", ".env").apply { writeText("JAVA_HOME=dummy\n") }
        expectThat(Env(logger, path))["JAVA_HOME"].isNotEqualTo("dummy")
    }

    @Test
    fun `should still provide access to system env missing env file`(logger: InMemoryLogger) {
        expectThat(Env(logger, Path.of("/nowhere"))).isNotEmpty()
    }
}
