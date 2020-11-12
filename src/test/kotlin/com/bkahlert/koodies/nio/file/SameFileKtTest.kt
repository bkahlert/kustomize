package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.string.random
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.exists
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class SameFileKtTest {
    @Test
    fun `should always return same path`() {
        val random = String.random(17)
        expectThat(sameFile(random)).isEqualTo(sameFile(random))
    }

    @Test
    fun `should not implicitly create file`() {
        val random = String.random(17)
        expectThat(sameFile(random)).not { exists() }
    }
}
