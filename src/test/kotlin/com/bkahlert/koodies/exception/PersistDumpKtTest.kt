package com.bkahlert.koodies.exception

import com.bkahlert.koodies.nio.file.readText
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempPath
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.string.LineSeparators
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.terminal.ansi.AnsiFormats.bold
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.MiscFixture
import com.imgcstmzr.util.hasContent
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.filter
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isFailure
import strikt.assertions.last
import strikt.assertions.single
import strikt.assertions.startsWith
import java.io.IOException

@Execution(CONCURRENT)
class PersistDumpKtTest {

    private val tempDir = tempDir().deleteOnExit()
    private val path get() = tempDir.tempPath(PersistDumpKtTest::class.toString(), ".log")
    private val data = { MiscFixture.BootingRaspberry.text }

    @Test
    fun `should dump data`() {
        val dumps = persistDump(path = path, data = data)
        expectThat(dumps.values.map { it.deleteOnExit().readText().removeEscapeSequences().lines().dropLast(1) }).hasSize(2).all {
            @Suppress("SpellCheckingInspection")
            first().startsWith("Booting QEMU machine \"versatilepb\" with kernel=")
            last().startsWith(" raspberrypi login:")
        }
    }

    @Test
    fun `should throw if data could not be dumped`() {
        val path = path.writeText("already exists")
        path.toFile().setReadOnly()
        expectCatching {
            persistDump(path = path.deleteOnExit(), data = data)
        }.isFailure().isA<IOException>()
        path.toFile().setWritable(true)
    }

    @Test
    fun `should dump IO to file with ansi formatting`() {
        val dumps = persistDump(path = path, data = { "ansi".bold() + LineSeparators.LF + "no ansi" }).values.onEach { it.deleteOnExit() }
        expectThat(dumps).filter { !it.serialized.endsWith("no-ansi.log") }.single().hasContent("""
                ${"ansi".bold()}
                no ansi
            """.trimIndent())
    }

    @Test
    fun `should dump IO to file without ansi formatting`() {
        val dumps = persistDump(path = path, data = { "ansi".bold() + LineSeparators.LF + "no ansi" }).values.onEach { it.deleteOnExit() }
        expectThat(dumps).filter { it.serialized.endsWith("no-ansi.log") }.single().hasContent("""
                ansi
                no ansi
            """.trimIndent())
    }
}
