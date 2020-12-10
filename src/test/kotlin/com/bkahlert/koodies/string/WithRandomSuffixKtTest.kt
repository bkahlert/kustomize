package com.bkahlert.koodies.string//import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.matches
import strikt.assertions.startsWith

@Execution(CONCURRENT)
class WithRandomSuffixKtTest {

    @Test
    fun `should add 4 random characters`() {
        val string = "the-string"
        expectThat(string.withRandomSuffix()) {
            startsWith("the-string-")
            matches(Regex("the-string-[0-9a-zA-Z]{4}"))
        }
    }

    @Test
    fun `should not append to already existing random suffix`() {
        val string = "the-string"
        expectThat(string.withRandomSuffix().withRandomSuffix()) {
            startsWith("the-string-")
            matches(Regex("the-string-[0-9a-zA-Z]{4}"))
        }
    }
}
