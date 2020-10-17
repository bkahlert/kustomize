package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
internal class AsStringKtTest {
    @Test
    internal fun `should produce same byte string as toString`() {
        val string = "【\uD83E\uDDDA\uD83C\uDFFF\u200D♀️〗"
        expectThat(StringBuilder(string).asString()).isEqualTo(string)
    }
}
