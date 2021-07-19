package com.imgcstmzr.cli

import com.github.ajalt.clikt.core.MissingOption
import com.github.ajalt.clikt.core.PrintHelpMessage
import com.imgcstmzr.expectRendered
import com.imgcstmzr.test.E2E
import com.typesafe.config.ConfigException
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
        fun `should use specified cache dir`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = resolve("empty.conf").writeText("""
                img {
                  name = project
                  os = RISC OS Pico RC5 (test only)
                }
            """.trimIndent())
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--cache-dir", pathString))
            expectThat(resolve("project")) {
                exists()
                resolve("download").exists() and {
                    get { listDirectoryEntriesRecursively() }.hasSize(1)
                }
            }
        }

        @Slow @Test
        fun `should render progress`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = resolve("empty.conf").writeText("""
                img {
                  name = project
                  os = RISC OS Pico RC5 (test only)
                }
            """.trimIndent())
            CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--cache-dir", pathString))
            expectRendered().ansiRemoved {
                contains("SUMMARY")
                contains("Image flashed to: —")
            }
        }

        @E2E @Test
        fun `should customize image`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val configFile = resolve("empty.conf").writeText("img { }")
            expectThrows<PrintHelpMessage> { CustomizeCommand().parse(arrayOf("--config-file", configFile.pathString, "--cache-dir", pathString)) }
        }
    }
}
