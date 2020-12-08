package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.imgcstmzr.patch.unformattedLog
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class MicroLoggerTest {

    @Test
    fun `should micro log`(logger: InMemoryLogger) {
        logger.logging("segment") {
            logLine { "something" }
            singleLineLogging("single") {
                microLogging {
                    logStatus { IO.Type.OUT typed "ABC" }
                    logLine { "" }
                    logLine { "123" }
                    "abc"
                }
                logLine { "456" }
                microLogging {
                    logStatus { IO.Type.OUT typed "XYZ" }
                    logLine { "" }
                    logLine { "789" }
                }
            }
            logLine { "something" }
        }

        expectThat(logger).unformattedLog.matchesCurlyPattern("""
            ╭─────╴{}
            │   
            │   
            │   ╭─────╴segment
            │   │   
            │   │   something
            │   │   single: (ABC ˃  ˃ 123 ˃ ✔ returned abc) 456 (XYZ ˃  ˃ 789 ˃ ✔) ✔
            │   │   something
            │   │
            │   ╰─────╴✔
            │
        """.trimIndent())
    }
}
