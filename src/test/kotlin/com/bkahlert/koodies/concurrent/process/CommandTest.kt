package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.test.strikt.toStringIsEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class CommandTest {
    
    @Test
    fun `should output callable command`() {
        expectThat(Command(listOf("2>&1"), "command", "-a", "--bee", "c")).toStringIsEqualTo("""
            2>&1 command \
            -a \
            --bee \
            c
        """.trimIndent())
    }
}
