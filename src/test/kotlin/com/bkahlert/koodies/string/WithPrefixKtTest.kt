package com.bkahlert.koodies.string//import static org.assertj.core.api.Assertions.assertThat;
import com.imgcstmzr.patch.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class WithPrefixKtTest {
    @Test
    fun `should prepend prefix if missing`() {
        expectThat("foo".withPrefix("bar")).isEqualTo("barfoo")
    }

    @Test
    fun `should fully prepend prefix if partially present`() {
        expectThat("rfoo".withPrefix("bar")).isEqualTo("barrfoo")
    }

    @Test
    fun `should not prepend prefix if present`() {
        expectThat("barfoo".withPrefix("bar")).isEqualTo("barfoo")
    }
}

