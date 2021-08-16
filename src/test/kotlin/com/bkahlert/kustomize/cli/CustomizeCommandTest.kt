package com.bkahlert.kustomize.cli

import com.bkahlert.kommons.io.copyToDirectory
import com.bkahlert.kommons.io.path.deleteOnExit
import com.bkahlert.kommons.io.path.deleteRecursively
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.io.path.writeText
import com.bkahlert.kommons.junit.UniqueId
import com.bkahlert.kommons.test.Slow
import com.bkahlert.kommons.test.expectThrows
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.text.ansiRemoved
import com.bkahlert.kustomize.Kustomize
import com.bkahlert.kustomize.TestKustomize.testCacheDirectory
import com.bkahlert.kustomize.expectRendered
import com.bkahlert.kustomize.os.OperatingSystems.HypriotOS
import com.bkahlert.kustomize.test.E2E
import com.github.ajalt.clikt.core.BadParameterValue
import com.github.ajalt.clikt.core.MissingOption
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.typesafe.config.ConfigException
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
            expectRendered().contains("Env: ${Kustomize.work.resolve(".env").toUri()}")
        }

        @Slow @Test
        fun `should create cache relative to workdir if specified relative path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = MinimalConfFixture.copyToDirectory(this)
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--skip-patches", "--env-file", "relative-env-file"))
            expectRendered().contains("Env: ${Kustomize.work.resolve("relative-env-file").toUri()}")
        }

        @Slow @Test
        fun `should create cache at absolute path if specified absolute path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = MinimalConfFixture.copyToDirectory(this)
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--skip-patches", "--env-file", resolve("absolute-env-file").pathString))
            expectRendered().contains("Env: ${resolve("absolute-env-file").toUri()}")
        }

        @AfterEach
        fun cleanUp() {
            Kustomize.work.resolve("cache").deleteRecursively()
            Kustomize.work.resolve("minimal").deleteRecursively()
        }
    }

    @Nested
    inner class CacheDirArgument {

        @Slow @Test
        fun `should create cache in workdir by default`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = MinimalConfFixture.copyToDirectory(this)
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--skip-patches"))
            expectThat(Kustomize.work / "minimal").containsImage("riscos.img")
        }

        @Slow @Test
        fun `should create cache relative to workdir if specified relative path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = MinimalConfFixture.copyToDirectory(this)
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--skip-patches", "--cache-dir", "cache"))
            expectThat(Kustomize.work / "cache" / "minimal").containsImage("riscos.img")
        }

        @Slow @Test
        fun `should create cache at absolute path if specified absolute path`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = MinimalConfFixture.copyToDirectory(this)
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--skip-patches", "--cache-dir", pathString))
            expectThat(this / "minimal").containsImage("riscos.img")
        }

        @AfterEach
        fun cleanUp() {
            Kustomize.work.resolve("cache").deleteRecursively()
            Kustomize.work.resolve("minimal").deleteRecursively()
        }
    }

    @Nested
    inner class ConfigFileArgument {

        @Test
        fun `should throw on missing argument`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThrows<MissingOption> { CustomizeCommand().parse(arrayOf("--jaeger-hostname", "localhost")) }
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
                expectThat(Kustomize.work.resolve("file-name").deleteOnExit(true)).isDirectory()
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
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--cache-dir", testCacheDirectory.pathString))
            expectRendered().ansiRemoved {
                contains("▶ Configuring")
                contains("Configuration: ${resolve("hello.conf").toUri()}")
                contains("Name: ${HypriotOS.name}")
                contains("OS: ${HypriotOS.fullName}")
                contains("Env: ${Kustomize.work.resolve(".env").toUri()}")
                contains("Cache: ${Kustomize.work.resolve(testCacheDirectory).toUri()}")

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
