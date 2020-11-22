package com.bkahlert.koodies.exception

import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempPath
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.regex.RegularExpressions
import com.bkahlert.koodies.regex.sequenceOfAllMatches
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.MiscFixture
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.all
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.hasSize
import strikt.assertions.startsWith
import java.net.URL

@Execution(CONCURRENT)
class DumpKtTest {

    private val tempDir = tempDir().deleteOnExit()
    private val path get() = tempDir.tempPath(DumpKtTest::class.toString(), ".log")
    private val data get() = MiscFixture.BootingRaspberry.text

    @Test
    fun `should contain explanation`() {
        expectThat(dump(null, path = path, data = data)).contains("A dump has been written")
    }

    @Test
    fun `should contain capitalized custom explanation next to default one`() {
        expectThat(dump("custom explanation", path = path, data = data))
            .contains("Custom explanation")
            .contains("A dump has been written")
    }

    @Test
    fun `should contain url pointing to dumps`() {
        expectThat(dump("", path = path, data = data)).get {
            RegularExpressions.urlRegex.sequenceOfAllMatches(this)
                .map { url -> URL(url) }
                .map { url -> url.openStream().reader().readLines() }
                .map { lines -> lines.first().removeEscapeSequences() to lines.dropLast(1).last().removeEscapeSequences() }
                .toList()
        }.hasSize(2).all {
            @Suppress("SpellCheckingInspection")
            get { first }.startsWith("Booting QEMU machine \"versatilepb\" with kernel=")
            get { second }.startsWith(" raspberrypi login:")
        }
    }

    @Test
    fun `should contains last lines of dump`() {
        expectThat(dump("", path = path, data = data)).get { lines().takeLast(11).map { it.trim() } }
            .containsExactly(
                "raspberry",
                "Login incorrect",
                "raspberrypi login:",
                "raspberrypi login:",
                "raspberrypi login:",
                "raspberrypi login:",
                "raspberrypi login:",
                "raspberrypi login:",
                "raspberrypi login:",
                "",
                ""
            )
    }

    @Test
    fun `should log all lines if problem saving the log`() {
        val path = path.writeText("already exists")
        path.toFile().setReadOnly()
        expectThat(dump("error message", path = path, data = data)) {
            get { lines().take(2) }.containsExactly("Error message", "In the attempt to persist the corresponding dump the following error occurred:")
            get { lines().drop(2).first() }.startsWith("FileNotFoundException: ")
            get { lines() }
                .any { contains("The not successfully persisted dump is as follows:") }
                .any {
                    @Suppress("SpellCheckingInspection")
                    contains("Booting QEMU machine \"versatilepb\" with kernel=")
                }
                .any { contains("[  OK  ] Started Load Kernel Modules.") }
                .any { contains("raspberrypi login:") }
        }
        path.toFile().setWritable(true)
    }
}
