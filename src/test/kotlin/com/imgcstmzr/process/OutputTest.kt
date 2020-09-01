package com.imgcstmzr.process

import com.imgcstmzr.process.Output.Companion.ofType
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Suppress("RedundantInnerClassModifier")
@Execution(ExecutionMode.CONCURRENT)
internal class OutputTest {

    @TestFactory
    internal fun `should with enclosing brackets`() = listOf("someone's output", "")
        .flatMap { rawOutput ->
            OutputType.values().map { type ->
                dynamicTest("$rawOutput + $type") {
                    val string = rawOutput.ofType(type).toString()
                    expectThat(string).isEqualTo("${type.symbol}⟨$rawOutput⟩")
                }
            }
        }

    @Test
    internal fun `should use symbol to abbreviate type`() {
        val string = "raw output".ofType(OutputType.META).toString()
        expectThat(string).isEqualTo("𝕄⟨raw output⟩")
    }
}


