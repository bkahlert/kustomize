package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.string.Grapheme
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.bkahlert.koodies.tracing.trace
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class MicroTracerKtTest {
    @Test
    fun `should micro seq`(logger: InMemoryLogger) {
        logger.subTrace<Any?>("segment") {
            trace("@")
            microTrace<Any?>(Grapheme("🤠")) {
                trace("a")
                trace("")
                trace("b c")
            }
            trace("@")
        }

        expectThat(logger.logged).matchesCurlyPattern("""
            ╭─────╴{}
            │   
            │   segment: @ (🤠 a ˃  ˃ b c) @ {}
        """.trimIndent())
    }
}
