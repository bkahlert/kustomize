package com.imgcstmzr.cli

import koodies.io.path.writeText
import koodies.io.randomFile
import koodies.junit.UniqueId
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
    fun `should read env file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path: Path = randomFile("personal", ".env").apply { writeText("A=B\n CC= DD \n") }
        expectThat(Env(path))
            .hasEntry("A", "B")
            .hasEntry("CC", "DD")
    }

    @Test
    fun `should favor environment variables`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val path: Path = randomFile("personal", ".env").apply { writeText("JAVA_HOME=dummy\n") }
        expectThat(Env(path))["JAVA_HOME"].isNotEqualTo("dummy")
    }

    @Test
    fun `should still provide access to system env missing env file`() {
        expectThat(Env(Path.of("/nowhere"))).isNotEmpty()
    }
}
