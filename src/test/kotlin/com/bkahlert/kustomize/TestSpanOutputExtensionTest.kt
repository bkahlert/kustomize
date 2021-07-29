package com.bkahlert.kustomize

import koodies.text.matchesCurlyPattern
import koodies.tracing.spanning
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo

class TestSpanOutputExtensionTest {

    @Test
    fun `should provide access to rendered output`() {
        val result = spanning("span name") {
            event("not rendered")
            log("rendered")
            42
        }
        expectThat(result).isEqualTo(42)
        expectRendered().matchesCurlyPattern("""
            ╭──╴span name
            │
            │   rendered                   
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun `should provide access to nested rendered output`() {
        val result = spanning("span name") {
            spanning("child span name") {
                event("not rendered")
                log("rendered")
                42
            }
        }
        expectThat(result).isEqualTo(42)
        expectRendered().matchesCurlyPattern("""
            ╭──╴span name
            │
            │   ╭──╴child span name
            │   │
            │   │   rendered                                                                
            │   │
            │   ╰──╴✔︎
            │
            ╰──╴✔︎
        """.trimIndent())
    }

    @Test
    fun `should provide access to consecutive rendered output`() {
        val result = spanning("span name") {
            event("not rendered")
            log("rendered")
            42
        } + spanning("span 2 name") {
            event("not rendered 2")
            log("rendered 2")
            37.5
        }
        expectThat(result).isEqualTo(79.5)
        expectRendered().matchesCurlyPattern("""
            ╭──╴span name
            │
            │   rendered
            │
            ╰──╴✔︎
            ╭──╴span 2 name
            │
            │   rendered 2
            │
            ╰──╴✔︎
        """.trimIndent())
    }
}
