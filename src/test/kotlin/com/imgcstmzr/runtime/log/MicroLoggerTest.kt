package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Disabled
@Execution(CONCURRENT)
class MicroLoggerTest {

    @Test
    fun `should micro log`(logger: InMemoryLogger<Any>) {
        logger.subLogger<Any?>("segment") {
            logLine { "@" }
            microLog<Any?> {
                logStatus { IO.Type.OUT typed "ABC" }
                logLine { "" }
                logLine { "123" }
                "xyz"
            }
            microLog<Any?> {
                logStatus { IO.Type.OUT typed "ABC" }
                logLine { "" }
                logLine { "123" }
            }
            logLine { "@" }
            singleLineLogger<Any?>("single") {
                microLog<Any?> {
                    logStatus { IO.Type.OUT typed "ABC" }
                    logLine { "" }
                    logLine { "123" }
                    "xyz"
                }
                microLog<Any?> {
                    logStatus { IO.Type.OUT typed "ABC" }
                    logLine { "" }
                    logLine { "123" }
                }
            }
            logLine { "@" }
        }

        expectThat(logger.logged).matchesCurlyPattern("""
            ╭─────╴{}
            │   
            ├─╴ segment: @ (🤠 a ˃  ˃ b c) @ {}
        """.trimIndent())
    }
}
