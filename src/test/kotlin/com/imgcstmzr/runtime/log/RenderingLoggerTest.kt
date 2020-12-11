package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.nio.file.readLines
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.string.Unicode
import com.bkahlert.koodies.string.matchesCurlyPattern
import com.bkahlert.koodies.string.repeat
import com.bkahlert.koodies.test.junit.test
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.containsAtMost
import com.imgcstmzr.util.logging.Columns
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.InMemoryLoggerFactory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import strikt.api.expect
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.endsWith
import strikt.assertions.first
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isSuccess

@Execution(CONCURRENT)
class RenderingLoggerTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log`() {
        logLine { "｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ" }
        logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logResult { Result.success(Unit) }

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭─────╴{}
                    │{}
                    │   ｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ▮▮
                    │{}
                    ╰─────╴✔{}
                """.trimIndent())
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should allow single line logging`() {
        logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        singleLineLogging("mini") {
            logLine { OUT typed "A" }
//            logException { RuntimeException("exception message") }
            logStatus { OUT typed "bb" }
            logStatus { OUT typed " " }
        }
        logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logResult { Result.success(Unit) }

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭─────╴{}
                    │{}
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ▮▮
                    │   mini: A bb   ✔
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ▮▮
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ▮▮
                    │{}
                    ╰─────╴✔{}
                """.trimIndent())
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should allow nested logging`() {
        logStatus { OUT typed "outer 1" }
        logStatus { OUT typed "outer 2" }
        logging("nested log", null) {
            logStatus { OUT typed "nested 1" }
            logStatus { OUT typed "nested 2" }
            logStatus { OUT typed "nested 3" }
        }
        logStatus { OUT typed "outer 3" }
        logStatus { OUT typed "outer 4" }
        logResult { Result.success("end") }

        expectThat(logged).matchesCurlyPattern("""
                    ╭─────╴{}
                    │{}
                    │   outer 1                                               {}                                      ▮▮
                    │   outer 2                                               {}                                      ▮▮
                    │{}
                    │   ╭─────╴nested log
                    │   │{}
                    │   │   nested 1                                          {}                                      ▮▮
                    │   │   nested 2                                          {}                                      ▮▮
                    │   │   nested 3                                          {}                                      ▮▮
                    │   │{}
                    │   ╰─────╴✔{}
                    │{}
                    │   outer 3                                               {}                                      ▮▮
                    │   outer 4                                               {}                                      ▮▮
                    │{}
                    ╰─────╴✔ returned{}
                """.trimIndent())
    }

    @Execution(SAME_THREAD)
    @TestFactory
    fun @receiver:Columns(100) InMemoryLoggerFactory.`should allow complex layout`() = listOf(
        true to """
            ╭─────╴{}
            │{}
            │   outer 1                                               {}                                      ▮▮
            │   outer 2{}
            │{}
            │   ╭─────╴nested log
            │   │{}
            │   │   nested 1                                          {}                                      ▮▮
            │   │   mini segment: 12345 sample ✔
            │   │{}
            │   │   ╭─────╴nested log
            │   │   │{}
            │   │   │   nested 1                                      {}                                      ▮▮
            │   │   │   mini segment: 12345 sample ✔
            │   │   │   nested 2                                      {}                                      ▮▮
            │   │   │   nested 3                                      {}                                      ▮▮
            │   │   │{}
            │   │   ╰─────╴✔{}
            │   │{}
            │   │   nested 2                                          {}                                      ▮▮
            │   │   nested 3                                          {}                                      ▮▮
            │   │{}
            │   ╰─────╴✔{}
            │{}
            │   outer 3                                               {}                                      ▮▮
            │   outer 4                                               {}                                      ▮▮
            │{}
            ╰─────╴✔{}
        """.trimIndent(),
        false to """
            Started: {}
             outer 1                                                  {}                                      ▮▮
             outer 2
             Started: nested log{}
              nested 1                                                {}                                      ▮▮
              mini segment: 12345 sample ✔{}
              Started: nested log{}
               nested 1                                               {}                                      ▮▮
               mini segment: 12345 sample ✔{}
               nested 2                                               {}                                      ▮▮
               nested 3                                               {}                                      ▮▮
              Completed: ✔{}
              nested 2                                                {}                                      ▮▮
              nested 3                                                {}                                      ▮▮
             Completed: ✔{}
             outer 3                                                  {}                                      ▮▮
             outer 4                                                  {}                                      ▮▮
            Completed: ✔{}
        """.trimIndent(),
    ).test("bordered={}") { (borderedOutput, expectation) ->
        val label = if (borderedOutput) "bordered" else "not-bordered"
        with(createLogger(label, borderedOutput = borderedOutput)) {
            logStatus { OUT typed "outer 1" }
            logLine { "outer 2" }
            logging("nested log") {
                logStatus { OUT typed "nested 1" }
                singleLineLogging("mini segment") {
                    logStatus { ERR typed "12345" }
                    logStatus { META typed "sample" }
                }
                logging("nested log") {
                    logStatus { OUT typed "nested 1" }
                    singleLineLogging("mini segment") {
                        logStatus { ERR typed "12345" }
                        logStatus { META typed "sample" }
                    }
                    logStatus { OUT typed "nested 2" }
                    logStatus { OUT typed "nested 3" }
                }
                logStatus { OUT typed "nested 2" }
                logStatus { OUT typed "nested 3" }
            }
            logStatus { OUT typed "outer 3" }
            logStatus { OUT typed "outer 4" }
            logResult { Result.success(Unit) }

            expectThat(logged).matchesCurlyPattern(expectation)
        }
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log status`() {
        logStatus(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logResult { Result.success(Unit) }

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭─────╴{}
                    │{}
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ◀◀ getting phone call
                    │{}
                    ╰─────╴✔{}
                """.trimIndent())
    }

    @Suppress("LongLine")
    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log status in same column`() {
        logStatus(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logging("nested", null) {
            logStatus(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        }
        logResult { Result.success(Unit) }

        expectThat(logged)
            .contains("│   ☎Σ⊂⊂(☉ω☉∩)                                                                                                    ◀◀ getting phone call")
            .contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                                ◀◀ getting phone call")
            .not { contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                                     ◀◀ getting phone call") } // too much indent
            .not { contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                           ◀◀ getting phone call") } // too few indent
    }

    @Suppress("LongLine")
    @Test
    fun @receiver:Columns(10) InMemoryLogger.`should not break status line`() {
        logStatus(listOf(StringStatus("1234567890"))) { OUT typed "abc....xyz" }
        logging("nested", null) {
            logStatus(listOf(StringStatus("123456789 01234567890"))) { OUT typed "abc....xyz" }
            logging("nested", null) {
                logStatus(listOf(StringStatus("1234567890 1234567890 1234567890 1234567890"))) { OUT typed "abc....xyz" }
            }
        }
        logResult { Result.success(Unit) }

        expectThat(logged)
            .contains("│   │   │   ab          ◀◀ 1234567890 1234…567890 1234567890")
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log exception`() {
        kotlin.runCatching {
            logStatus { OUT typed "outer 1" }
            logStatus { OUT typed "outer 2" }
            logging<Any>("nested log", null) {
                logStatus { OUT typed "nested 1" }
                throw IllegalStateException("an exception")
            }
            logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logResult { Result.success("success") }
        }

        expectThat(logged).matchesCurlyPattern(
            """
                    ╭─────╴{}
                    │{}
                    │   outer 1                                               {}                                      ▮▮
                    │   outer 2                                               {}                                      ▮▮
                    │{}
                    │   ╭─────╴nested log
                    │   │{}
                    │   │   nested 1                                          {}                                      ▮▮
                    │   ϟ{}
                    │   ╰─────╴failed with IllegalStateException: an exception at.(${RenderingLoggerTest::class.simpleName}.kt:{}){}
                    │{}
                """.trimIndent(), ignoreTrailingLines = true)
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should simple log when closed twice`() {
        logResult { Result.success(Unit) }
        logResult { Result.success(Unit) }
        expectThat(logged)
            .containsAtMost("╰─────╴", 1)
            .contains("✔")
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should simply log multiple calls to logResult`() {
        expectCatching {
            singleLineLogging("close twice") {
                logStatus { META typed "line" }
                logResult { Result.success(1) }
                logResult { Result.success(2) }
                3
            }
        }.isSuccess()
        expectThat(logged).matchesCurlyPattern("""
            ╭─────╴{}
            │   
            │   close twice: line ✔ returned 1
            │   close twice: line ✔ returned 1 ✔ returned 2
            │   close twice: line ✔ returned 1 ✔ returned 2 ✔ returned 3
        """.trimIndent())
    }

    @Test
    fun InMemoryLogger.`should wrap long lines`() {
        val status: (String) -> HasStatus = {
            object : HasStatus {
                override fun status(): String = it
            }
        }
        val shortLine = "┬┴┬┴┤(･_├┬┴┬┴"
        val longLine = "｀、ヽ｀ヽ｀、ヽ".repeat(10) + "ノ＞＜)ノ" + " ｀、ヽ｀、ヽ｀、ヽ".repeat(10)
        logLine { shortLine }
        logLine { longLine }
        logStatus(listOf(status(shortLine))) { OUT typed shortLine }
        logStatus(listOf(status(shortLine))) { OUT typed longLine }
        logStatus(listOf(status(longLine))) { OUT typed shortLine }
        logStatus(listOf(status(longLine))) { OUT typed longLine }
        logResult { Result.success(longLine) }

        expectThat(logged).matchesCurlyPattern(
            """
                ╭─────╴{}
                │   
                │   ┬┴┬┴┤(･_├┬┴┬┴
                │   ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽノ＞＜)ノ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀
                │   、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ
                │   ┬┴┬┴┤(･_├┬┴┬┴                                                         ◀◀ ┬┴┬┴┤(･_├┬┴┬┴
                │   ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀          ◀◀ ┬┴┬┴┤(･_├┬┴┬┴
                │   ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽノ＞＜)ノ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀          
                │   、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀          
                │   、ヽ｀、ヽ                                                                 
                │   ┬┴┬┴┤(･_├┬┴┬┴                                                         ◀◀ ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ…ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ
                │   ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀          ◀◀ ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ…ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ
                │   ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽノ＞＜)ノ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀          
                │   、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀          
                │   、ヽ｀、ヽ                                                                 
                │
                ╰─────╴✔ returned ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽノ＞＜)ノ ｀、ヽ｀、ヽ
                ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ
                """.trimIndent())
    }

    @Test
    fun @receiver:Columns(100) InMemoryLogger.`should log to file`() {
        logLine { "｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ" }
        logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        val file = tempDir.tempFile("file-log", ".log")
        fileLogging(file, "Some logging heavy operation") {
            logLine { "line" }
            logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logException { RuntimeException("just a test") }
            logCaughtException { RuntimeException("covered") }
            "👍"
        }
        logLine { "Normal logging continues..." }
        logResult { Result.success(Unit) }

        expect {
            that(logged).matchesCurlyPattern(
                """
                    ╭─────╴{}
                    │{}
                    │   ｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ▮▮
                    │{}
                    │   ╭─────╴Some logging heavy operation{}
                    │   │{}
                    │   │   This process might produce pretty much log messages. Logging to …
                    │   │   ${Unicode.Emojis.pageFacingUp} ${file.toUri()}
                    │   │{}
                    │   ╰─────╴✔ returned 👍
                    │{}
                    │   Normal logging continues...
                    │{}
                    ╰─────╴✔{}
                """.trimIndent())

            that(file.readLines().filter { it.isNotBlank() }) {
                first().isEqualTo("Started: Some logging heavy operation")
                get { last { it.isNotBlank() } }.endsWith("👍")
            }
        }
    }

    @Suppress("UNREACHABLE_CODE")
    @Test
    fun `should show full exception only on outermost logger`() {
        val logger = InMemoryLogger("root", false, -1, emptyList())
        expect {
            catching {
                logger.logging("level 0") {
                    logLine { "doing stuff" }
                    logging("level 1") {
                        logLine { "doing stuff" }
                        logging("level 2") {
                            logLine { "doing stuff" }
                            throw RuntimeException("something happened\nsomething happened #2\nsomething happened #3")
                            logStatus { OUT typed "doing stuff" }
                            2
                        }
                        logLine { "doing stuff" }
                    }
                    logLine { "doing stuff" }
                }
            }.isFailure().isA<RuntimeException>()

            logger.logged.matchesCurlyPattern("""
                Started: root
                 Started: level 0
                  doing stuff
                  Started: level 1
                   doing stuff
                   Started: level 2
                    doing stuff
                   ϟ failed with RuntimeException: something happened at.(RenderingLoggerTest.kt:{})
                  ϟ failed with RuntimeException: something happened at.(RenderingLoggerTest.kt:{})
                 ϟ failed with RuntimeException: something happened at.(RenderingLoggerTest.kt:{})
            """.trimIndent())
        }
    }
}
