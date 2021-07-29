package com.bkahlert.kustomize.cli

import com.bkahlert.kustomize.Kustomize.WorkingDirectory
import com.bkahlert.kustomize.TestKustomize.TestCacheDirectory
import com.bkahlert.kustomize.expectRendered
import com.bkahlert.kustomize.os.OperatingSystems.HypriotOS
import com.bkahlert.kustomize.test.E2E
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.MissingOption
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.typesafe.config.ConfigException
import koodies.io.copyToDirectory
import koodies.io.path.deleteOnExit
import koodies.io.path.deleteRecursively
import koodies.io.path.pathString
import koodies.io.path.writeText
import koodies.junit.UniqueId
import koodies.test.Slow
import koodies.test.expectThrows
import koodies.test.withTempDir
import koodies.text.ansiRemoved
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isNotNull
import strikt.assertions.message
import strikt.java.isDirectory
import kotlin.io.path.div

class CustomizeCommandTest {

    @Nested
    inner class WithNoArguments {

        @Test
        fun `should print help`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThrows<PrintHelpMessage> { CustomizeCommand().parse(emptyArray()) }
        }
    }

    @Nested
    inner class EnvFileArgument {

        @Slow @Test
        fun `should create cache in workdir by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = MinimalConfFixture.copyToDirectory(this)
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--skip-patches"))
            expectRendered().contains("Env: ${WorkingDirectory.resolve(".env").toUri()}")
        }

        @Slow @Test
        fun `should create cache relative to workdir if specified relative path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = MinimalConfFixture.copyToDirectory(this)
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--skip-patches", "--env-file", "relative-env-file"))
            expectRendered().contains("Env: ${WorkingDirectory.resolve("relative-env-file").toUri()}")
        }

        @Slow @Test
        fun `should create cache at absolute path if specified absolute path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = MinimalConfFixture.copyToDirectory(this)
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--skip-patches", "--env-file", resolve("absolute-env-file").pathString))
            expectRendered().contains("Env: ${resolve("absolute-env-file").toUri()}")
        }

        @AfterEach
        fun cleanUp() {
            WorkingDirectory.resolve("cache").deleteRecursively()
            WorkingDirectory.resolve("minimal").deleteRecursively()
        }
    }

    @Nested
    inner class CacheDirArgument {

        @Slow @Test
        fun `should create cache in workdir by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = MinimalConfFixture.copyToDirectory(this)
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--skip-patches"))
            expectThat(WorkingDirectory / "minimal").containsImage("riscos.img")
        }

        @Slow @Test
        fun `should create cache relative to workdir if specified relative path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = MinimalConfFixture.copyToDirectory(this)
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--skip-patches", "--cache-dir", "cache"))
            expectThat(WorkingDirectory / "cache" / "minimal").containsImage("riscos.img")
        }

        @Slow @Test
        fun `should create cache at absolute path if specified absolute path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = MinimalConfFixture.copyToDirectory(this)
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--skip-patches", "--cache-dir", pathString))
            expectThat(this / "minimal").containsImage("riscos.img")
        }

        @AfterEach
        fun cleanUp() {
            WorkingDirectory.resolve("cache").deleteRecursively()
            WorkingDirectory.resolve("minimal").deleteRecursively()
        }
    }

    @Nested
    inner class ConfigFileArgument {

        @Test
        fun `should throw on missing argument`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThrows<MissingOption> { CustomizeCommand().parse(arrayOf("--reuse-last-working-copy")) }
        }

        @Test
        fun `should throw on missing config file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThrows<BadParameterValue> { CustomizeCommand().parse(arrayOf("--config-file", "missing.conf")) }
        }

        @Test
        fun `should throw on broken config file`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = resolve("broken.conf").writeText("""
                {
            """.trimIndent())
            expectThrows<ConfigException.Parse> { CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString)) }
        }

        @Nested
        inner class IncompleteConfig {

            @Test
            fun `should use file name on missing name`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val configFile = resolve("file-name.conf").writeText("""
                    os = RISC OS Pico RC5 (test only)
                """.trimIndent())
                CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString))
                expectThat(WorkingDirectory.resolve("file-name").deleteOnExit(true)).isDirectory()
            }

            @Test
            fun `should throw on missing os`(uniqueId: UniqueId) = withTempDir(uniqueId) {
                val configFile = resolve("incomplete.conf").writeText("")
                expectThrows<IllegalArgumentException> { CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString)) } that {
                    message.isNotNull().ansiRemoved.contains("Missing configuration: os")
                }
            }
        }

        @E2E @Test
        fun `should customize image`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = resolve("hello.conf").writeText("""
                name = ${HypriotOS.name}
                os = ${HypriotOS.fullName}
                setup = [
                  {
                    name: setup things
                    scripts: [
                      {
                        name: Greet
                        content: "echo '👏 🤓 👋'"
                      }
                    ]
                  },
                ]
            """.trimIndent())
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--cache-dir", TestCacheDirectory.pathString))
            expectRendered().ansiRemoved {
                contains("▶ Configuring")
                contains("Configuration: ${resolve("hello.conf").toUri()}")
                contains("Name: ${HypriotOS.name}")
                contains("OS: ${HypriotOS.fullName}")
                contains("Env: ${WorkingDirectory.resolve(".env").toUri()}")
                contains("Cache: ${WorkingDirectory.resolve(TestCacheDirectory).toUri()}")

                contains("▶ Preparing")
                contains("· ▶ Retrieving image")

                contains("▶ Applying 1 patches to Hypriot OS")
                contains("░░░░░░░ GREET")
                contains("👏 🤓 👋")
                contains("System halted")
            }

            // if all worked, delete temp dir immediately because of image size
            deleteRecursively()
        }
    }
}
