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
internal class RenderingLoggerIntTest {

    @Test
    internal fun `should log`(logger: InMemoryLogger<Unit>) {
        logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.logResult { Result.success(Unit) }

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
        logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.miniSegment<Unit, Unit>("mini") {
            logStatus { OUT typed "A" }
            logStatus { OUT typed "bb" }
            logStatus { OUT typed " " }
        }
        logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.logResult { Result.success(Unit) }

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
        logger.logStatus { OUT typed "outer 1" }
        logger.logStatus { OUT typed "outer 2" }
        logger.segment<String, Unit>("nested log", null) {
            logStatus { OUT typed "nested 1" }
            logStatus { OUT typed "nested 2" }
            logStatus { OUT typed "nested 3" }
        }
        logger.logStatus { OUT typed "outer 3" }
        logger.logStatus { OUT typed "outer 4" }
        logger.logResult { Result.success("end") }

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
                logger.logStatus { OUT typed "outer 1" }
                logger.logStatus { OUT typed "outer 2" }
                logger.segment<Unit, Unit>("nested log") {
                    logStatus { OUT typed "nested 1" }
                    miniSegment<Unit, Unit>("mini segment") {
                        logStatus { ERR typed "12345" }
                        logStatus { META typed "sample" }
                    }
                    segment<Unit, Unit>("nested log") {
                        logStatus { OUT typed "nested 1" }
                        miniSegment<Unit, Unit>("mini segment") {
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

                expectThat(logger.logged).matches(expectation)
            }
        }

    @Test
    internal fun `should log status`(logger: InMemoryLogger<Unit>) {
        logger.logStatus(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.logResult { Result.success(Unit) }

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
        logger.logStatus(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.segment<Unit, Unit>("nested", null) {
            logStatus(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        }
        logger.logResult { Result.success(Unit) }

        expectThat(logger.logged)
            .contains("│   ☎Σ⊂⊂(☉ω☉∩)                                                                                          ◀◀ getting phone call")
            .contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                      ◀◀ getting phone call")
            .not { contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                          ◀◀ getting phone call") } // too much indent
            .not { contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                    ◀◀ getting phone call") } // too few indent
    }

    @Test
    internal fun `should log exception`(logger: InMemoryLogger<String>) {
        kotlin.runCatching {
            logger.logStatus { OUT typed "outer 1" }
            logger.logStatus { OUT typed "outer 2" }
            logger.segment<String, Unit>("nested log", null) {
                logStatus { OUT typed "nested 1" }
                throw IllegalStateException("an exception")
            }
            logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logger.logStatus { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logger.logResult { Result.success("success") }
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
                    │   ╰─────╴Failure(nested log): java.lang.IllegalStateException: an exception @ ${RenderingLoggerIntTest::class.qualifiedName}{}
                    │{}
                    │   java.lang.IllegalStateException: an exception
                    │   	at {} 
                """.trimIndent(), ignoreTrailingLines = true)
    }

    @Test
    internal fun `should simple log when closed twice`(logger: InMemoryLogger<Unit>) {
        logger.logResult { Result.success(Unit) }
        logger.logResult { Result.success(Unit) }
        expectThat(logger.logged)
            .containsAtMost("╰─────╴")
            .contains("✔")
    }

    @Test
    internal fun `should simply log multiple calls to logLast`(logger: InMemoryLogger<String>) {
        expectCatching {
            logger.miniSegment<String, Int>("close twice") {
                logStatus { META typed "line" }
                logResult { Result.success(1) }
                logResult { Result.success(2) }
                3
            }
        }.isSuccess()
        expectThat(logger.logged).matches("""
            ╭─────╴{}
            │   
            ├─╴ close twice: line ✔ returned ❬1{}❭
            ├─╴ close twice: line ✔ returned ❬1{}❭ ✔ returned ❬2{}❭
            ├─╴ close twice: line ✔ returned ❬1{}❭ ✔ returned ❬2{}❭ ✔ returned ❬3{}❭
        """.trimIndent())
    }
}
