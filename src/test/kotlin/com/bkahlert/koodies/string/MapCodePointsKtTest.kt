package com.bkahlert.koodies.string

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.containsExactly

@Execution(CONCURRENT)
class MapCodePointsKtTest {
    @Test
    fun `should map all unicode points`() {
        expectThat("Az09Αω𝌀𝍖".mapCodePoints { "|${it.string}|" })
            .containsExactly("|A|", "|z|", "|0|", "|9|", "|Α|", "|ω|", "|𝌀|", "|𝍖|")
    }
}

