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
        logger.logLineLambda { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.logLastLambda { Result.success(Unit) }

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
        logger.logLineLambda { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.miniSegment<Unit, Unit>("mini") {
            logLineLambda { OUT typed "A" }
            logLineLambda { OUT typed "bb" }
            logLineLambda { OUT typed " " }
        }
        logger.logLineLambda { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.logLineLambda { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.logLastLambda { Result.success(Unit) }

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
        logger.logLineLambda { OUT typed "outer 1" }
        logger.logLineLambda { OUT typed "outer 2" }
        logger.segment<String, Unit>("nested log", null) {
            logLineLambda { OUT typed "nested 1" }
            logLineLambda { OUT typed "nested 2" }
            logLineLambda { OUT typed "nested 3" }
        }
        logger.logLineLambda { OUT typed "outer 3" }
        logger.logLineLambda { OUT typed "outer 4" }
        logger.logLastLambda { Result.success("end") }

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
                logger.logLineLambda { OUT typed "outer 1" }
                logger.logLineLambda { OUT typed "outer 2" }
                logger.segment<Unit, Unit>("nested log") {
                    logLineLambda { OUT typed "nested 1" }
                    miniSegment<Unit, Unit>("mini segment") {
                        logLineLambda { ERR typed "12345" }
                        logLineLambda { META typed "sample" }
                    }
                    segment<Unit, Unit>("nested log") {
                        logLineLambda { OUT typed "nested 1" }
                        miniSegment<Unit, Unit>("mini segment") {
                            logLineLambda { ERR typed "12345" }
                            logLineLambda { META typed "sample" }
                        }
                        logLineLambda { OUT typed "nested 2" }
                        logLineLambda { OUT typed "nested 3" }
                    }
                    logLineLambda { OUT typed "nested 2" }
                    logLineLambda { OUT typed "nested 3" }
                }
                logger.logLineLambda { OUT typed "outer 3" }
                logger.logLineLambda { OUT typed "outer 4" }
                logger.logLastLambda { Result.success(Unit) }

                expectThat(logger.logged).matches(expectation)
            }
        }

    @Test
    internal fun `should log status`(logger: InMemoryLogger<Unit>) {
        logger.logLineLambda(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.logLastLambda { Result.success(Unit) }

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
        logger.logLineLambda(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        logger.segment<Unit, Unit>("nested", null) {
            logLineLambda(listOf(StringStatus("getting phone call"))) { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
        }
        logger.logLastLambda { Result.success(Unit) }

        expectThat(logger.logged)
            .contains("│   ☎Σ⊂⊂(☉ω☉∩)                                                                                          ◀◀ getting phone call")
            .contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                      ◀◀ getting phone call")
            .not { contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                          ◀◀ getting phone call") } // too much indent
            .not { contains("│   │   ☎Σ⊂⊂(☉ω☉∩)                                                                                    ◀◀ getting phone call") } // too few indent
    }

    @Test
    internal fun `should log exception`(logger: InMemoryLogger<String>) {
        kotlin.runCatching {
            logger.logLineLambda { OUT typed "outer 1" }
            logger.logLineLambda { OUT typed "outer 2" }
            logger.segment<String, Unit>("nested log", null) {
                logLineLambda { OUT typed "nested 1" }
                throw IllegalStateException("an exception")
            }
            logger.logLineLambda { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logger.logLineLambda { OUT typed "☎Σ⊂⊂(☉ω☉∩)" }
            logger.logLastLambda { Result.success("success") }
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
        logger.logLastLambda { Result.success(Unit) }
        logger.logLastLambda { Result.success(Unit) }
        expectThat(logger.logged)
            .containsAtMost("╰─────╴")
            .contains("✔")
    }

    @Test
    internal fun `should simply log multiple calls to logLast`(logger: InMemoryLogger<String>) {
        expectCatching {
            logger.miniSegment<String, Int>("close twice") {
                logLineLambda { META typed "line" }
                logLastLambda { Result.success(1) }
                logLastLambda { Result.success(2) }
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
