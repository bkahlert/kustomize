package com.imgcstmzr.runtime.log

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.string.Grapheme
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
internal class MicroTracerKtTest {
    @Test
    internal fun `should micro seq`(logger: InMemoryLogger<Unit>) {
        logger.miniSegment<Unit, Unit>("segment") {
            logStatus { IO.Type.OUT typed "@" }
            microSequence(Grapheme("🤠")) {
                trace("a")
                trace("")
                trace("b c")
            }
            logStatus { IO.Type.OUT typed "@" }
        }

        expectThat(logger).matches("""
            ╭─────╴{}
            │   
            ├─╴ segment: @ (🤠 a ˃  ˃ b c) @ {}
            │
            ╰─────╴{}
        """.trimIndent())
    }

    @Test
    internal fun `should micro trace`(logger: InMemoryLogger<Unit>) {
        logger.miniTrace(::`should micro trace`) {
            trace("X")
            microTrace<Unit>(Grapheme("🤠")) {
                trace("a")
                trace("")
                trace("b c")
            }
            trace("Y")
        }

        expectThat(logger).matches("""
            ╭─────╴{}
            │   
            ├─╴ ${MicroTracerKtTest::class.simpleName}.${::`should micro trace`.name}(${InMemoryLogger::class.simpleName}): X (🤠 a ˃  ˃ b c) Y {}
            │
            ╰─────╴{}
        """.trimIndent())
    }
}
