package com.bkahlert.koodies.string

import com.imgcstmzr.patch.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat

@Execution(CONCURRENT)
class CodePointSequenceKtTest {
    @Test
    fun `should contain all unicode points`() {
        expectThat("Az09Αω𝌀𝍖".codePointSequence())
            .get { map { it.string }.joinToString("") }
            .isEqualTo("Az09Αω𝌀𝍖")
    }
}
