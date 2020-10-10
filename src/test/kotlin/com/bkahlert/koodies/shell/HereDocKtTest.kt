package com.bkahlert.koodies.shell

import com.bkahlert.koodies.test.strikt.matches
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
internal class HereDocKtTest {

    @Test
    internal fun `should create here document using given prefix and line separator`() {
        val hereDoc = listOf("line 1", "line 2").toHereDoc("MY-PREFIX", "␤")
        expectThat(hereDoc).isEqualTo("<<MY-PREFIX␤line 1␤line 2␤MY-PREFIX")
    }

    @Test
    internal fun `should create here document using HERE- prefix and line feed by default`() {
        val hereDoc = listOf("line 1", "line 2").toHereDoc()
        expectThat(hereDoc).matches("""
            <<HERE-{}
            line 1
            line 2
            HERE-{}
        """.trimIndent())
    }

    @Test
    internal fun `should accept empty list`() {
        val hereDoc = listOf<String>().toHereDoc()
        expectThat(hereDoc).matches("""
            <<HERE-{}
            HERE-{}
        """.trimIndent())
    }

    @Nested
    inner class Support {
        @Test
        internal fun `for Array`() {
            val hereDoc = arrayOf("line 1", "line 2").toHereDoc()
            expectThat(hereDoc).matches("""
            <<HERE-{}
            line 1
            line 2
            HERE-{}
        """.trimIndent())
        }

        @Test
        internal fun `for Iterable`() {
            val hereDoc = listOf("line 1", "line 2").asIterable().toHereDoc()
            expectThat(hereDoc).matches("""
            <<HERE-{}
            line 1
            line 2
            HERE-{}
        """.trimIndent())
        }
    }
}
