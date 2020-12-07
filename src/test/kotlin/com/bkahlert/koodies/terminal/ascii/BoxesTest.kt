package com.bkahlert.koodies.terminal.ascii

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.string.repeat
import com.imgcstmzr.runtime.log.matches
import com.imgcstmzr.util.logging.Columns
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class BoxesTest {
    @Test
    fun `should render FAIL`(@Columns(150) logger: InMemoryLogger) {
        logger.logLine { IO.Type.ERR typed Boxes.FAIL.toString() }
        expectThat(logger).matches("""
            ╭─────╴BoxesTest ➜ should render FAIL{}
            │   
            │   ▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄▄
            │   ████▌▄▌▄▐▐▌█████
            │   ████▌▄▌▄▐▐▌▀████
            │   ▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀▀
            │
            ╰─────╴✔ 
        """.trimIndent())
    }

    @Test
    fun `should render sphere box`(@Columns(100) logger: InMemoryLogger) {
        logger.logLine { IO.Type.META typed Boxes.SPHERICAL("SPHERICAL") }
        expectThat(logger).matches("""
            ╭─────╴BoxesTest ➜ should render sphere box{}
            │   
            │     █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏  ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
            │   █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ${'\u00A0'.repeat(3)}SPHERICAL${'\u00A0'.repeat(3)}  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
            │     █ ▉▕▊▕▋▕▌▕▍▕▎▕▏ ▏  ▏  ▕  ▕  ▏▕▎▕▍▕▌▕▋▕▊▕▉ █
            │
            ╰─────╴✔ 
        """.trimIndent())
    }

    @Test
    fun `should render single line sphere box`(@Columns(100) logger: InMemoryLogger) {
        logger.logLine { IO.Type.META typed Boxes.SINGLE_LINE_SPHERICAL("SINGLE LINE SPHERICAL") }
        expectThat(logger).matches("""
            ╭─────╴BoxesTest ➜ should render single line sphere box{}
            │   
            │    ▕  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █ ▇ ▆ ▅ ▄ ▃ ▂ ▁  SINGLE LINE SPHERICAL  ▁ ▂ ▃ ▄ ▅ ▆ ▇ █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ ▕  
            │
            ╰─────╴✔ 
        """.trimIndent())
    }

    @Test
    fun `should render wide pillars`(@Columns(100) logger: InMemoryLogger) {
        logger.logLine { IO.Type.META typed Boxes.WIDE_PILLARS("WIDE PILLARS") }
        expectThat(logger).matches("""
            ╭─────╴BoxesTest ➜ should render wide pillars{}
            │   
            │   █ █ ▉▕▉ ▊▕▊▕▋ ▋▕▌ ▌ ▍▕▎ ▍ ▎▕▏ ▏ WIDE PILLARS  ▏ ▏▕▎ ▍ ▎▕▍ ▌ ▌▕▋ ▋▕▊▕▊ ▉▕▉ █ █
            │
            ╰─────╴✔ 
        """.trimIndent())
    }

    @Test
    fun `should render pillars`(@Columns(100) logger: InMemoryLogger) {
        logger.logLine { IO.Type.META typed Boxes.PILLARS("PILLARS") }
        expectThat(logger).matches("""
            ╭─────╴BoxesTest ➜ should render pillars{}
            │   
            │   █ ▉ ▊ ▋ ▌ ▍ ▎ ▏ PILLARS  ▏ ▎ ▍ ▌ ▋ ▊ ▉ █
            │
            ╰─────╴✔ 
        """.trimIndent())
    }
}
