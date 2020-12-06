package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.tempPath
import com.bkahlert.koodies.string.LineSeparators.LF
import com.bkahlert.koodies.string.quoted
import com.bkahlert.koodies.test.strikt.toStringIsEqualTo
import com.bkahlert.koodies.time.sleep
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isEqualTo
import java.nio.file.Path
import kotlin.time.seconds
import org.codehaus.plexus.util.cli.Commandline as PlexusCommandLine

@Execution(CONCURRENT)
class CommandLineTest {

    @Nested
    inner class LazyStartedProcess {

        @Test
        fun `should not start on its own`() {
            val (_, file) = createLazyFileCreatingProcess()
            2.seconds.sleep()
            expectThat(file).not { exists() }
        }

        @Test
        fun `should start if accessed`() {
            val (process, file) = createLazyFileCreatingProcess()
            process.info()
            2.seconds.sleep()
            expectThat(file).exists()
        }

        @Test
        fun `should start if processed`() {
            val (process, file) = createLazyFileCreatingProcess()
            process.silentlyProcess()
            2.seconds.sleep()
            expectThat(file).exists()
        }
    }

    @Test
    fun `should run`() {
        val command = CommandLine("echo", "test")
        expectThat(command) {
            continuationsRemoved.isEqualTo("echo test")
            evaluatesTo("test", 0)
        }
    }

    @Test
    fun `should run with more arguments`() {
        val command = CommandLine("echo", "one", "two", "three")
        expectThat(command) {
            continuationsRemoved.isEqualTo("echo one two three")
            evaluatesTo("one two three", 0)
        }
    }

    @Nested
    inner class Expansion {

        @Test
        fun `should expand`() {
            val command = CommandLine("echo", "\$HOME")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \$HOME")
                evaluated.output.isEqualTo(System.getProperty("user.home"))
                evaluated.exitValue.isEqualTo(0)
            }
        }

        @Test
        fun `should not expand`() {
            val command = CommandLine("echo", "\\\$HOME")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \\\$HOME")
                not { evaluated.output.isEqualTo(System.getProperty("user.home")) }
                evaluated.exitValue.isEqualTo(0)
            }
        }
    }

    @Nested
    inner class Formatting {
        @Test
        fun `should output formatted`() {
            expectThat(CommandLine("command", "-a", "--bee", "c", "x y z".quoted)).toStringIsEqualTo("""
            command \
            -a \
            --bee \
            c \
            "x y z"
        """.trimIndent())
        }

        @Test
        fun `should handle whitespaces correctly command`() {
            expectThat(CommandLine("command", " - a", "    ", "c c", "x y z".quoted)).toStringIsEqualTo("""
            command \
            "- a" \
             \
            "c c" \
            "x y z"
        """.trimIndent())
        }

        @Test
        fun `should handle nesting`() {
            expectThat(CommandLine(
                "command",
                "-a",
                "--bee",
                CommandLine("command", "-a", "--bee", "c", "x y z".quoted).toString(),
                "x y z".quoted)
            ).toStringIsEqualTo("""
            command \
            -a \
            --bee \
            "command \
            -a \
            --bee \
            c \
            \"x y z\"" \
            "x y z"
        """.trimIndent())
        }
    }

    @Nested
    inner class Quoting {

        @Test
        fun `should not quote unnecessarily`() {
            val command = CommandLine("echo", "Hello")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo Hello")
                evaluatesTo("Hello", 0)
            }
        }

        @Test
        fun `should quote on whitespaces`() {
            val command = CommandLine("echo", "Hello World!")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \"Hello World!\"")
                evaluatesTo("Hello World!", 0)
            }
        }

        @Test
        fun `should support single quotes`() {
            val command = CommandLine("echo", "'\$HOME'")
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo '\$HOME'")
                evaluatesTo("\$HOME", 0)
            }
        }
    }

    @Nested
    inner class Nesting {

        @Test
        fun `should produce runnable output`() {
            val nestedCommand = CommandLine("echo", "Hello")
            val command = CommandLine("echo", nestedCommand.toString())
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \"echo Hello\"")
                evaluated.output.isEqualTo("echo Hello")
                evaluated.evaluatesTo("Hello", 0)
            }
        }

        @Test
        fun `should produce runnable quoted output`() {
            val nestedCommand = CommandLine("echo", "Hello World!")
            val command = CommandLine("echo", nestedCommand.toString())
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \"echo \\\"Hello World!\\\"\"")
                evaluated.output.isEqualTo("echo \"Hello World!\"")
                evaluated.evaluatesTo("Hello World!", 0)
            }
        }

        @Test
        fun `should produce runnable single quoted output`() {
            val nestedCommand = CommandLine("echo", "'Hello World!'")
            val command = CommandLine("echo", nestedCommand.toString())
            expectThat(command) {
                continuationsRemoved.isEqualTo("echo \"echo \\\"'Hello World!'\\\"\"")
                evaluated.output.isEqualTo("echo \"'Hello World!'\"")
                evaluated.evaluatesTo("'Hello World!'", 0)
            }
        }
    }
}

val Assertion.Builder<CommandLine>.continuationsRemoved
    get() = get("continuation removed %s") { toString().replace("\\s+\\\\.".toRegex(RegexOption.DOT_MATCHES_ALL), " ") }

val Assertion.Builder<CommandLine>.evaluated
    get() = get("evaluated %s") {
        val commandLine = this.toString()
        Processes.evalShellScript {
            !commandLine
        }
    }

val Assertion.Builder<LoggedProcess>.output
    get() = get("output %s") {
        output.unformatted.lines().drop(2).joinToString(LF)
    }

val <P : Process> Assertion.Builder<P>.exitValue
    get() = get("exit value %s") { exitValue() }

fun Assertion.Builder<CommandLine>.evaluatesTo(expectedOutput: String, expectedExitValue: Int) {
    with(evaluated) {
        output.isEqualTo(expectedOutput)
        exitValue.isEqualTo(expectedExitValue)
    }
}

val Assertion.Builder<LoggedProcess>.evaluatedProcess
    get() = output.get("parsed command line %s") { PlexusCommandLine(this) }.get("execute %s") { execute() }

val Assertion.Builder<Process>.processOutput
    get() = get("all output %s") { inputStream.reader().readText().trim() }

val Assertion.Builder<Process>.processExitValue
    get() = get("exit value %s") { waitFor() }

@JvmName("evaluatesToLoggedProcess")
fun Assertion.Builder<LoggedProcess>.evaluatesTo(expectedOutput: String, expectedExitValue: Int) {
    exitValue.isEqualTo(0)
    with(evaluatedProcess) {
        processOutput.isEqualTo(expectedOutput)
        processExitValue.isEqualTo(expectedExitValue)
    }
}

fun createLazyFileCreatingProcess(): Pair<DelegatingProcess, Path> {
    val nonExistingFile = tempPath(extension = ".txt").deleteOnExit()
    val fileCreatingCommandLine = CommandLine("touch", nonExistingFile.serialized)
    return fileCreatingCommandLine.lazyStart() to nonExistingFile
}
