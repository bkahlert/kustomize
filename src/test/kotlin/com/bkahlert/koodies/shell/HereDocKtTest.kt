package com.bkahlert.koodies.shell

import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
class HereDocKtTest {

    @Test
    fun `should create here document using given prefix and line separator`() {
        val hereDoc = listOf("line 1", "line 2").toHereDoc("MY-PREFIX", "␤")
        expectThat(hereDoc).isEqualTo("<<MY-PREFIX␤line 1␤line 2␤MY-PREFIX")
    }

    @Test
    fun `should create here document using HERE- prefix and line feed by default`() {
        val hereDoc = listOf("line 1", "line 2").toHereDoc()
        expectThat(hereDoc).matchesCurlyPattern("""
            <<HERE-{}
            line 1
            line 2
            HERE-{}
        """.trimIndent())
    }

    @Test
    fun `should accept empty list`() {
        val hereDoc = listOf<String>().toHereDoc()
        expectThat(hereDoc).matchesCurlyPattern("""
            <<HERE-{}
            HERE-{}
        """.trimIndent())
    }

    @Nested
    inner class Support {
        @Test
        fun `for Array`() {
            val hereDoc = arrayOf("line 1", "line 2").toHereDoc()
            expectThat(hereDoc).matchesCurlyPattern("""
            <<HERE-{}
            line 1
            line 2
            HERE-{}
        """.trimIndent())
        }

        @Test
        fun `for Iterable`() {
            val hereDoc = listOf("line 1", "line 2").asIterable().toHereDoc()
            expectThat(hereDoc).matchesCurlyPattern("""
            <<HERE-{}
            line 1
            line 2
            HERE-{}
        """.trimIndent())
        }
    }
}
