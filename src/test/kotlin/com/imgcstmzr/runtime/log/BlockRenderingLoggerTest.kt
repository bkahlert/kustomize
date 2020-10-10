package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.test.strikt.matches
import com.imgcstmzr.process.Output.Type.ERR
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.process.Output.Type.OUT
import com.imgcstmzr.util.containsAtMost
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.InMemoryLoggerFactory
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isSuccess

@Execution(ExecutionMode.CONCURRENT)
internal class BlockRenderingLoggerTest {

    @Test
    internal fun `should log`(logger: InMemoryLogger<Unit>) {
        logger.logLine(OUT typed "☎Σ⊂⊂(☉ω☉∩)")
        logger.logLast(Result.success(Unit))

        expectThat(logger.logged).matches(
            """
                    ╭─────╴{}
                    │{}
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ▮▮
                    │{}
                    ╰─────╴✔{}
                """.trimIndent())
    }

    @Test
    internal fun `should allow single line logging`(logger: InMemoryLogger<Unit>) {
        logger.logLine(OUT typed "☎Σ⊂⊂(☉ω☉∩)")
        logger.miniSegment<Unit, Unit>("mini") {
            logLine(OUT typed "A")
            logLine(OUT typed "bb")
            logLine(OUT typed " ")
        }
        logger.logLine(OUT typed "☎Σ⊂⊂(☉ω☉∩)")
        logger.logLine(OUT typed "☎Σ⊂⊂(☉ω☉∩)")
        logger.logLast(Result.success(Unit))

        expectThat(logger.logged).matches(
            """
                    ╭─────╴{}
                    │{}
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ▮▮
                    ├─╴ mini: A bb   ✔
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ▮▮
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ▮▮
                    │{}
                    ╰─────╴✔{}
                """.trimIndent())
    }

    @Test
    internal fun `should allow nested logging`(logger: InMemoryLogger<String>) {
        logger.logLine(OUT typed "outer 1")
        logger.logLine(OUT typed "outer 2")
        logger.segment<String, Unit>("nested log", null) {
            logLine(OUT typed "nested 1")
            logLine(OUT typed "nested 2")
            logLine(OUT typed "nested 3")
        }
        logger.logLine(OUT typed "outer 3")
        logger.logLine(OUT typed "outer 4")
        logger.logLast(Result.success("end"))

        expectThat(logger.logged).matches("""
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
                    ╰─────╴✔{}
                """.trimIndent())
    }

    @ConcurrentTestFactory
    internal fun `should allow complex layout`(loggerFactory: InMemoryLoggerFactory<Unit>) = listOf(
        true to """
            ╭─────╴{}
            │{}
            │   outer 1                                               {}                                      ▮▮
            │   outer 2                                               {}                                      ▮▮
            │{}
            │   ╭─────╴nested log
            │   │{}
            │   │   nested 1                                          {}                                      ▮▮
            │   ├─╴ mini segment: 12345 sample ✔
            │   │{}
            │   │   ╭─────╴nested log
            │   │   │{}
            │   │   │   nested 1                                      {}                                      ▮▮
            │   │   ├─╴ mini segment: 12345 sample ✔
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
             outer 2                                                  {}                                      ▮▮
             Started: nested log{}
              nested 1                                                {}                                      ▮▮
              :mini segment: 12345 sample ✔{}
              Started: nested log{}
               nested 1                                               {}                                      ▮▮
               :mini segment: 12345 sample ✔{}
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
                logger.logLine(OUT typed "outer 1")
                logger.logLine(OUT typed "outer 2")
                logger.segment<Unit, Unit>("nested log") {
                    logLine(OUT typed "nested 1")
                    miniSegment<Unit, Unit>("mini segment") {
                        logLine(ERR typed "12345")
                        logLine(META typed "sample")
                    }
                    segment<Unit, Unit>("nested log") {
                        logLine(OUT typed "nested 1")
                        miniSegment<Unit, Unit>("mini segment") {
                            logLine(ERR typed "12345")
                            logLine(META typed "sample")
                        }
                        logLine(OUT typed "nested 2")
                        logLine(OUT typed "nested 3")
                    }
                    logLine(OUT typed "nested 2")
                    logLine(OUT typed "nested 3")
                }
                logger.logLine(OUT typed "outer 3")
                logger.logLine(OUT typed "outer 4")
                logger.logLast(Result.success(Unit))

                expectThat(logger.logged).matches(expectation)
            }
        }

    @Test
    internal fun `should log status`(logger: InMemoryLogger<Unit>) {
        logger.logLine(OUT typed "☎Σ⊂⊂(☉ω☉∩)", listOf(StringStatus("getting phone call")))
        logger.logLast(Result.success(Unit))

        expectThat(logger.logged).matches(
            """
                    ╭─────╴{}
                    │{}
                    │   ☎Σ⊂⊂(☉ω☉∩)                                            {}                                      ◀◀ getting phone call
                    │{}
                    ╰─────╴✔{}
                """.trimIndent())
    }

    @Test
    internal fun `should log status in same column`(logger: InMemoryLogger<Unit>) {
        logger.logLine(OUT typed "☎Σ⊂⊂(☉ω☉∩)", listOf(StringStatus("getting phone call")))
        logger.segment<Unit, Unit>("nested", null) {
            logLine(OUT typed "☎Σ⊂⊂(☉ω☉∩)", listOf(StringStatus("getting phone call")))
        }
        logger.logLast(Result.success(Unit))

        expectThat(logger.logged)
            .contains("│   ☎Σ⊂⊂(☉ω☉∩)                                                                                          ◀◀ getting phone call")
            .contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                      ◀◀ getting phone call")
            .not { contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                          ◀◀ getting phone call") } // too much indent
            .not { contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                    ◀◀ getting phone call") } // too few indent
    }

    @Test
    internal fun `should log exception`(logger: InMemoryLogger<String>) {
        kotlin.runCatching {
            logger.logLine(OUT typed "outer 1")
            logger.logLine(OUT typed "outer 2")
            logger.segment<String, Unit>("nested log", null) {
                logLine(OUT typed "nested 1")
                throw IllegalStateException("an exception")
            }
            logger.logLine(OUT typed "☎Σ⊂⊂(☉ω☉∩)")
            logger.logLine(OUT typed "☎Σ⊂⊂(☉ω☉∩)")
            logger.logLast(Result.success("success"))
        }

        expectThat(logger.logged).matches(
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
                    │   ╰─────╴Failure(nested log): java.lang.IllegalStateException: an exception @ ${BlockRenderingLoggerTest::class.qualifiedName}{}
                    │{}
                    │   java.lang.IllegalStateException: an exception
                    │   	at {} 
                """.trimIndent(), ignoreTrailingLines = true)
    }

    @Test
    internal fun `should simple log when closed twice`(logger: InMemoryLogger<Unit>) {
        logger.logLast(Result.success(Unit))
        logger.logLast(Result.success(Unit))
        expectThat(logger.logged)
            .containsAtMost("╰─────╴")
            .contains("✔")
    }

    @Test
    internal fun `should simply log multiple calls to logLast`(logger: InMemoryLogger<String>) {
        expectCatching {
            logger.miniSegment<String, Int>("close twice") {
                logLast(Result.success(1))
                logLast(Result.success(2))
                3
            }
        }.isSuccess()
        expectThat(logger.logged).matches("""
            ╭─────╴{}
            │   
            ├─╴ close twice: ✔ with ❬1{}❭
            ├─╴ close twice: ✔ with ❬2{}❭
            ├─╴ close twice: ✔ with ❬3{}❭
        """.trimIndent())
    }
}
