package com.imgcstmzr.cli

import com.github.ajalt.clikt.core.MissingOption
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.imgcstmzr.expectRendered
import com.imgcstmzr.test.E2E
import com.typesafe.config.ConfigException
import koodies.io.path.deleteRecursively
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.pathString
import koodies.io.path.writeText
import koodies.junit.UniqueId
import koodies.test.Slow
import koodies.test.expectThrows
import koodies.test.withTempDir
import koodies.text.ansiRemoved
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.hasSize
import strikt.assertions.isNotNull
import strikt.assertions.message
import strikt.java.exists
import strikt.java.resolve

class CustomizeCommandTest {

    @Nested
    inner class WithNoArguments {

        @Test
        fun `should print help`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThrows<PrintHelpMessage> { CustomizeCommand().parse(emptyArray()) }
        }
    }

    @Nested
    inner class WithMissingConfig {

        @Test
        fun `should throw`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expectThrows<MissingOption> { CustomizeCommand().parse(arrayOf("--reuse-last-working-copy")) }
        }
    }

    @Nested
    inner class WithBrokenConfig {

        @Test
        fun `should throw`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = resolve("empty.conf").writeText("""
                img {
            """.trimIndent())
            expectThrows<ConfigException.Parse> { CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString)) }
        }
    }

    @Nested
    inner class WithIncompleteArguments {

        @Test
        fun `should throw on missing name`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = resolve("empty.conf").writeText("""
                img {
                  os = RISC OS Pico RC5 (test only)
                }
            """.trimIndent())
            expectThrows<IllegalArgumentException> { CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString)) } that {
                message.isNotNull().ansiRemoved.contains("Missing configuration img.name")
            }
        }

        @Test
        fun `should throw on missing os`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = resolve("empty.conf").writeText("""
                img {
                  name = project
                }
            """.trimIndent())
            expectThrows<IllegalArgumentException> { CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString)) } that {
                message.isNotNull().ansiRemoved.contains("Missing configuration img.os")
            }
        }
    }

    @Nested
    inner class WithValidArguments {

        @Slow @Test
        fun `should download image to specified cache dir`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = resolve("empty.conf").writeText("""
                img {
                  name = project
                  os = RISC OS Pico RC5 (test only)
                }
            """.trimIndent())
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--cache-dir", pathString))
            expectThat(resolve("project")) {
                exists()
                resolve("raw").exists() and {
                    get { listDirectoryEntriesRecursively() }.hasSize(1)
                }
            }
        }

        @E2E @Test
        fun `should customize image`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = resolve("hello.conf").writeText("""
                img {
                  name = project
                  os = Hypriot OS
                    setup = [
                      {
                        name: say hello
                        scripts: [
                          {
                            name: "greet"
                            content: "echo 'Hey 👋'"
                          }
                        ]
                      },
                    ]
                }
            """.trimIndent())
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--cache-dir", pathString))
            expectRendered().ansiRemoved {
                contains("▶ Configuring")
                contains("Configuration: file://")
                contains("Name: project")
                contains("OS: Hypriot OS")
                contains("Env: file://")
                contains("Cache: file://")

                contains("▶ Preparing")
                contains("· ▶ Retrieving image")

                contains("▶ Applying 1 patches to Hypriot OS")
                contains("░░░░░░░ GREET")
                contains("Hey 👋")
                contains("System halted")
            }

            // if all worked, delete temp dir immediately because of image size
            deleteRecursively()
        }
    }
}
