package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.builder.ListBuilder
import com.bkahlert.koodies.concurrent.process.IO.Type.ERR
import com.bkahlert.koodies.concurrent.process.IO.Type.META
import com.bkahlert.koodies.concurrent.process.IO.Type.OUT
import com.bkahlert.koodies.docker.DockerImage
import com.bkahlert.koodies.docker.DockerRepository
import com.bkahlert.koodies.docker.DockerRunCommandLineBuilder.Companion.buildRunCommand
import com.bkahlert.koodies.docker.MountOption
import com.bkahlert.koodies.docker.OptionsBuilder
import com.bkahlert.koodies.nio.file.readLines
import com.bkahlert.koodies.nio.file.tempDir
import com.bkahlert.koodies.nio.file.tempFile
import com.bkahlert.koodies.string.Unicode
import com.bkahlert.koodies.string.repeat
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.util.FixtureLog.deleteOnExit
import com.imgcstmzr.util.containsAtMost
import com.imgcstmzr.util.logging.Columns
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.InMemoryLoggerFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest
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
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess

@Execution(CONCURRENT)
class RenderingLoggerTest {

    private val tempDir = tempDir().deleteOnExit()

    @Test
    fun `should log`(@Columns(100) logger: InMemoryLogger) {
        logger.logLine { "｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ" }
        logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.logResult { Result.success(Unit) }

        expectThat(logger.logged).matchesCurlyPattern(
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
    fun `should allow single line logging`(@Columns(100) logger: InMemoryLogger) {
        logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.singleLineLogger("mini") {
            logLine { OUT typed "A" }
//            logException { RuntimeException("exception message") }
            logStatus { OUT typed "bb" }
            logStatus { OUT typed " " }
        }
        logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.logResult { Result.success(Unit) }

        expectThat(logger.logged).matchesCurlyPattern(
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
    fun `should allow nested logging`(@Columns(100) logger: InMemoryLogger) {
        logger.logStatus { OUT typed "outer 1" }
        logger.logStatus { OUT typed "outer 2" }
        logger.subLogger("nested log", null) {
            logStatus { OUT typed "nested 1" }
            logStatus { OUT typed "nested 2" }
            logStatus { OUT typed "nested 3" }
        }
        logger.logStatus { OUT typed "outer 3" }
        logger.logStatus { OUT typed "outer 4" }
        logger.logResult { Result.success("end") }

        expectThat(logger.logged).matchesCurlyPattern("""
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
    fun `should allow complex layout`(@Columns(100) loggerFactory: InMemoryLoggerFactory) = listOf(
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
    )
        .map { (borderedOutput, expectation) ->
            val label = if (borderedOutput) "bordered" else "not-bordered"
            val logger = loggerFactory.createLogger(label, borderedOutput = borderedOutput)
            dynamicTest("should allow complex layout—$label") {
                logger.logStatus { OUT typed "outer 1" }
                logger.logLine { "outer 2" }
                logger.subLogger("nested log") {
                    logStatus { OUT typed "nested 1" }
                    singleLineLogger("mini segment") {
                        logStatus { ERR typed "12345" }
                        logStatus { META typed "sample" }
                    }
                    subLogger("nested log") {
                        logStatus { OUT typed "nested 1" }
                        singleLineLogger("mini segment") {
                            logStatus { ERR typed "12345" }
                            logStatus { META typed "sample" }
                        }
                        logStatus { OUT typed "nested 2" }
                        logStatus { OUT typed "nested 3" }
                    }
                    logStatus { OUT typed "nested 2" }
                    logStatus { OUT typed "nested 3" }
                }
                logger.logStatus { OUT typed "outer 3" }
                logger.logStatus { OUT typed "outer 4" }
                logger.logResult { Result.success(Unit) }

                expectThat(logger.logged).matchesCurlyPattern(expectation)
            }
        }

    @Test
    fun `should log status`(@Columns(100) logger: InMemoryLogger) {
        logger.logStatus(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.logResult { Result.success(Unit) }

        expectThat(logger.logged).matchesCurlyPattern(
            """
                    ╭─────╴{}
                    │{}
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ◀◀ getting phone call
                    │{}
                    ╰─────╴✔{}
                """.trimIndent())
    }

    @Test
    fun `should log status in same column`(@Columns(100) logger: InMemoryLogger) {
        logger.logStatus(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.subLogger("nested", null) {
            logStatus(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        }
        logger.logResult { Result.success(Unit) }

        expectThat(logger.logged)
            .contains("│   ☎Σ⊂⊂(☉ω☉∩)                                                                                                    ◀◀ getting phone call")
            .contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                                ◀◀ getting phone call")
            .not { contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                                     ◀◀ getting phone call") } // too much indent
            .not { contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                           ◀◀ getting phone call") } // too few indent
    }

    @Test
    fun `should log exception`(@Columns(100) logger: InMemoryLogger) {
        kotlin.runCatching {
            logger.logStatus { OUT typed "outer 1" }
            logger.logStatus { OUT typed "outer 2" }
            logger.subLogger<Any>("nested log", null) {
                logStatus { OUT typed "nested 1" }
                throw IllegalStateException("an exception")
            }
            logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logger.logResult { Result.success("success") }
        }

        expectThat(logger.logged).matchesCurlyPattern(
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
    fun `should simple log when closed twice`(@Columns(100) logger: InMemoryLogger) {
        logger.logResult { Result.success(Unit) }
        logger.logResult { Result.success(Unit) }
        expectThat(logger.logged)
            .containsAtMost("╰─────╴", 1)
            .contains("✔")
    }

    @Test
    fun `should simply log multiple calls to logResult`(@Columns(100) logger: InMemoryLogger) {
        expectCatching {
            logger.singleLineLogger("close twice") {
                logStatus { META typed "line" }
                logResult { Result.success(1) }
                logResult { Result.success(2) }
                3
            }
        }.isSuccess()
        expectThat(logger.logged).matchesCurlyPattern("""
            ╭─────╴{}
            │   
            │   close twice: line ✔ returned 1
            │   close twice: line ✔ returned 1 ✔ returned 2
            │   close twice: line ✔ returned 1 ✔ returned 2 ✔ returned 3
        """.trimIndent())
    }

    @Test
    fun `should wrap long lines`(logger: InMemoryLogger) {
        val status: (String) -> HasStatus = {
            object : HasStatus {
                override fun status(): String = it
            }
        }
        val shortLine = "┬┴┬┴┤(･_├┬┴┬┴"
        val longLine = "｀、ヽ｀ヽ｀、ヽ".repeat(10) + "ノ＞＜)ノ" + " ｀、ヽ｀、ヽ｀、ヽ".repeat(10)
        logger.logLine { shortLine }
        logger.logLine { longLine }
        logger.logStatus(listOf(status(shortLine))) { OUT typed shortLine }
        logger.logStatus(listOf(status(shortLine))) { OUT typed longLine }
        logger.logStatus(listOf(status(longLine))) { OUT typed shortLine }
        logger.logStatus(listOf(status(longLine))) { OUT typed longLine }
        logger.logResult { Result.success(longLine) }

        expectThat(logger.logged).matchesCurlyPattern(
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
                │   ┬┴┬┴┤(･_├┬┴┬┴                                                         ◀◀ ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ…、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ
                │   ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀          ◀◀ ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ…、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ
                │   ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽノ＞＜)ノ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀          
                │   、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀          
                │   、ヽ｀、ヽ                                                                 
                │
                ╰─────╴✔ returned ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽ｀、ヽ｀ヽ｀、ヽノ＞＜)ノ ｀、ヽ｀、ヽ
                ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ ｀、ヽ｀、ヽ｀、ヽ
                """.trimIndent())
    }

    @Test
    fun `should log to file`(@Columns(100) logger: InMemoryLogger) {
        logger.logLine { "｀、ヽ｀ヽ｀、ヽ(ノ＞＜)ノ ｀、ヽ｀☂ヽ｀、ヽ" }
        logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        val file = tempDir.tempFile("file-log", ".log")
        logger.fileLogger(file, "Some logging heavy operation") {
            logLine { "line" }
            logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logException { RuntimeException("just a test") }
            logCaughtException { RuntimeException("covered") }
            "👍"
        }
        logger.logLine { "Normal logging continues..." }
        logger.logResult { Result.success(Unit) }

        expect {
            that(logger.logged).matchesCurlyPattern(
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

    fun calc(arg: String): Int {
        return arg.toInt()
    }

    @Test
    fun `should show full exception only on outermost logger`() {

        val logger = InMemoryLogger(caption = "level 0", borderedOutput = true, statusInformationColumn = -1, outputStreams = emptyList())


        fun calc(arg: String): Int = logging {
            arg.toInt()
        }

        fun calc2(arg: String): Int = logging {
            arg.toInt()
        }

        val x: String = logging {
            val calc2: Int = calc2("2")
            calc2.toString()
        }

        logger.subLogger("level 1") {
            "dd"

            4
        }

        DockerImage.image(DockerRepository.of("")).buildRunCommand {
            options {
                testExtension()
                mounts {
                    test2Extension()
                }
            }
        }
    }
}

fun zzzz(s: String): Int {
    return 0
}

fun <T, R> T.logging(block: T.() -> R): R {
//    return this.call(arg)
    return null as R
}

fun (CharSequence.() -> Int).xxx() {
    invoke("s")
}

fun OptionsBuilder.testExtension() {}

fun (ListBuilder<MountOption>).test2Extension() {}
