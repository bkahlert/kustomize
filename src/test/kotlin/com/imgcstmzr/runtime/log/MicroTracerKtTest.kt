package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.string.Grapheme
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.bkahlert.koodies.tracing.trace
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.logging.expectThatLogged
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT

@Execution(CONCURRENT)
class MicroTracerKtTest {
    @Test
    fun InMemoryLogger.`should micro seq`() {
        subTrace<Any?>("segment") {
            trace("@")
            microTrace<Any?>(Grapheme("🤠")) {
                trace("a")
                trace("")
                trace("b c")
            }
            trace("@")
        }

        expectThatLogged().matchesCurlyPattern("""
            ╭─────╴{}
            │   
            │   segment: @ (🤠 a ˃  ˃ b c) @ {}
        """.trimIndent())
    }
}
