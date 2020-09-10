package com.imgcstmzr.process

import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import com.imgcstmzr.process.Output.Type.META
import com.imgcstmzr.util.stripOffAnsi
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
    internal fun `should leave content untouched`() = listOf("someone's output", "")
        .flatMap { rawOutput ->
            Output.Type.values().map { type ->
                dynamicTest("$rawOutput + $type") {
                    val string = (type typed rawOutput).toString()
                    expectThat(string.stripOffAnsi()).isEqualTo(rawOutput)
                }
            }
        }

    @Test
    internal fun `should properly format`() {
        val string = (META typed "raw output").toString()
        expectThat(string).isEqualTo((tc.gray + tc.italic)("raw output"))
    }
}


