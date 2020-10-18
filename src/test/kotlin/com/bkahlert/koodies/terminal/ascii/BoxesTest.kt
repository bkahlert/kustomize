package com.bkahlert.koodies.terminal.ascii

import com.imgcstmzr.process.Output
import com.imgcstmzr.runtime.log.matches
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
internal class BoxesTest {
    @Test
    internal fun `should render FAIL`(logger: InMemoryLogger<Unit>) {
        logger.logLineLambda { Output.Type.ERR typed Boxes.FAIL.toString() }
        expectThat(logger).matches("""
            ╭─────╴should render FAIL{}
            │   
            │   ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄{}
            │   ████▌▄▌▄▐▐▌█████
            │   ████▌▄▌▄▐▐▌▀████
            │   ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
            │
            ╰─────╴✔ 
        """.trimIndent())
    }
}
