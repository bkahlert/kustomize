package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.startAsDaemon
import com.bkahlert.koodies.nio.file.readText
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.time.poll
import com.bkahlert.koodies.time.sleep
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.hasContent
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.contains
import strikt.assertions.filter
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNotEmpty
import strikt.assertions.single
import java.io.IOException
import java.nio.file.Path
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class IOLogTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should provide thread-safe access to log`() {
        var stop = false
        val ioLog = IOLog()
        startAsDaemon {
            var i = 0
            while (!stop) {
                ioLog.add(IO.Type.META, "being busy $i times\n".toByteArray())
                10.milliseconds.sleep()
                i++
            }
        }

        poll { ioLog.logged.isNotEmpty() }.every(10.milliseconds).forAtMost(1.seconds) { fail("No I/O logged in one second.") }

        expectThat(ioLog.logged) {
            isNotEmpty()
            contains(IO.Type.META typed "being busy 0 times")
        }
        stop = true
    }

    @Nested
    inner class DumpIO {

        private val ioLog = createIOLog()

        @Test
        fun `should dump IO to specified file`() {
            val dumps: Map<String, Path> = ioLog.dump(tempDir.tempFile(extension = ".log"))
            expectThat(dumps.values.map { it.readText().removeEscapeSequences() }).hasSize(2).all {
                isEqualTo("""
                Starting process...
                processing
                awaiting input: 
                cancel
                invalid input
                an abnormal error has occurred (errno 99)
            """.trimIndent())
            }
        }

        @Test
        fun `should throw if IO could not be dumped`() {
            val logPath = tempDir.tempFile(extension = ".log").writeText("already exists")
            logPath.toFile().setReadOnly()
            expectCatching { ioLog.dump(logPath) }.isFailure().isA<IOException>()
            logPath.toFile().setWritable(true)
        }

        @Test
        fun `should dump IO to file with ansi formatting`() {
            val dumps = ioLog.dump(1234).values.onEach { it.deleteOnExit() }
            expectThat(dumps).filter { !it.serialized.endsWith("no-ansi.log") }.single().hasContent("""
                ${IO.Type.META.format("Starting process...")}
                ${IO.Type.OUT.format("processing")}
                ${IO.Type.OUT.format("awaiting input: ")}
                ${IO.Type.IN.format("cancel")}
                ${IO.Type.ERR.format("invalid input")}
                ${IO.Type.ERR.format("an abnormal error has occurred (errno 99)")}
            """.trimIndent())
        }

        @Test
        fun `should dump IO to file without ansi formatting`() {
            val dumps = ioLog.dump(1234).values.onEach { it.deleteOnExit() }
            expectThat(dumps).filter { it.serialized.endsWith("no-ansi.log") }.single().hasContent("""
                Starting process...
                processing
                awaiting input: 
                cancel
                invalid input
                an abnormal error has occurred (errno 99)
            """.trimIndent())
        }
    }
}

fun createIOLog(): IOLog = IOLog().apply {
    add(IO.Type.META, "Starting process...\n".toByteArray())
    add(IO.Type.OUT, "processing\n".toByteArray())
    add(IO.Type.OUT, "awaiting input: \n".toByteArray())
    add(IO.Type.IN, "cancel\n".toByteArray())
    add(IO.Type.ERR, "invalid input\n".toByteArray())
    add(IO.Type.ERR, "an abnormal error has occurred (errno 99)\n".toByteArray())
}
