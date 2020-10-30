package com.bkahlert.koodies.collections//import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isFailure

@Execution(CONCURRENT)
internal class RemoveFirstKtTest {
    @Test
    internal fun `should remove first elements`() {
        val list = mutableListOf("a", "b", "c")
        val first2 = list.removeFirst(2)
        expectThat(first2).containsExactly("a", "b")
        expectThat(list).containsExactly("c")
    }

    @Test
    internal fun `should throw and leave list unchanged on too few elements`() {
        val list = mutableListOf("a", "b", "c")
        expectCatching { list.removeFirst(4) }.isFailure().isA<IllegalArgumentException>()
        expectThat(list).containsExactly("a", "b", "c")
    }
}
