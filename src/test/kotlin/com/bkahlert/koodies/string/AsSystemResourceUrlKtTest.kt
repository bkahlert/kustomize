package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class AsSystemResourceUrlKtTest {
    @Test
    fun `should resolve to valid URL`() {
        expectThat("cmdline.txt".asSystemResourceUrl().readBytes().size).isEqualTo(169)
    }
}
