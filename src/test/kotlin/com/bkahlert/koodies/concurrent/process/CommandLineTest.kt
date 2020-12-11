package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.tempPath
import com.bkahlert.koodies.string.quoted
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
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
import kotlin.time.milliseconds
import kotlin.time.seconds
import org.codehaus.plexus.util.cli.Commandline as PlexusCommandLine
import java.lang.Process as JavaProcess

@Execution(CONCURRENT)
class CommandLineTest {

    @Nested
    inner class Equality {
        @Test
        fun `should equal based on command and arguments`() {
            val cmdLine1 = CommandLine("/bin/command", "arg1", "arg 2")
            val cmdLine2 = CommandLine(Path.of("/bin/command"), "arg1", "arg 2")
            expectThat(cmdLine1).isEqualTo(cmdLine2)
        }
    }

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
            process.alive
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

    @Test
    fun `should provide summary`() {
        expectThat(CommandLine(
            "!ls", "-lisa",
            "!mkdir", "-p", "/shared",
            "!mkdir", "-p", "/shared/guestfish.shared/boot",
            "-copy-out", "/boot/cmdline.txt", "/shared/guestfish.shared/boot",
            "!mkdir", "-p", "/shared/guestfish.shared/non",
            "-copy-out", "/non/existing.txt", "/shared/guestfish.shared/non",
        ).summary).matchesCurlyPattern("◀◀ lisa  ◀ mkdir  ◀ …  ◀ mkdir")
    }
}

val Assertion.Builder<CommandLine>.continuationsRemoved
    get() = get("continuation removed %s") { toString().replace("\\s+\\\\.".toRegex(RegexOption.DOT_MATCHES_ALL), " ") }

val Assertion.Builder<CommandLine>.evaluated: Assertion.Builder<LightweightProcess>
    get() = get("evaluated %s") {
        LightweightProcess(this).also { it.output }
    }

val Assertion.Builder<LightweightProcess>.output
    get() = get("output %s") { output }

val <P : LightweightProcess> Assertion.Builder<P>.exitValue
    get() = get("exit value %s") { exitValue }

fun Assertion.Builder<CommandLine>.evaluatesTo(expectedOutput: String, expectedExitValue: Int) {
    with(evaluated) {
        output.isEqualTo(expectedOutput)
        50.milliseconds.sleep()
        exitValue.isEqualTo(expectedExitValue)
    }
}

val Assertion.Builder<LightweightProcess>.evaluatedProcess
    get() = output.get("parsed command line %s") { PlexusCommandLine(this) }.get("execute %s") { execute() }

val Assertion.Builder<JavaProcess>.processOutput
    get() = get("all output %s") { inputStream.reader().readText().trim() }

val Assertion.Builder<JavaProcess>.processExitValue
    get() = get("exit value %s") { waitFor() }

@JvmName("evaluatesToLightweightProcess")
fun Assertion.Builder<LightweightProcess>.evaluatesTo(expectedOutput: String, expectedExitValue: Int) {
    with(evaluatedProcess) {
        processOutput.isEqualTo(expectedOutput)
        processExitValue.isEqualTo(expectedExitValue)
    }
    exitValue.isEqualTo(0)
}

fun createLazyFileCreatingProcess(): Pair<ManagedProcess, Path> {
    val nonExistingFile = tempPath(extension = ".txt").deleteOnExit()
    val fileCreatingCommandLine = CommandLine("touch", nonExistingFile.serialized)
    return ManagedProcess(fileCreatingCommandLine) to nonExistingFile
}
