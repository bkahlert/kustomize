package com.bkahlert.koodies.test.junit

import com.bkahlert.koodies.test.junit.SystemProperty.Companion.flag
import com.bkahlert.koodies.test.junit.systemproperties.SystemProperty
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(ExecutionMode.CONCURRENT)
internal class SystemPropertyTest {

    @Nested
    inner class FlagSystemProperty {

        @Nested
        inner class WithEnabledUnlessFalseOrEmpty {

            internal val enabledUnlessFalseOrEmpty by flag(defaultValue = true)

            @Test
            internal fun `should evaluate to false on absent value`() {
                expectThat(enabledUnlessFalseOrEmpty).isTrue()
            }

            @Test
            @SystemProperty(name = "enabledUnlessFalseOrEmpty", value = "true")
            internal fun `should evaluate to false on "true"`() {
                expectThat(enabledUnlessFalseOrEmpty).isFalse()
            }

            @Test
            @SystemProperty(name = "enabledUnlessFalseOrEmpty", value = "false")
            internal fun `should evaluate to true on "false"`() {
                expectThat(enabledUnlessFalseOrEmpty).isTrue()
            }

            @Test
            @SystemProperty(name = "enabledUnlessFalseOrEmpty", value = "")
            internal fun `should evaluate to true on empty value`() {
                expectThat(enabledUnlessFalseOrEmpty).isTrue()
            }

            @Test
            @SystemProperty(name = "enabledUnlessFalseOrEmpty", value = "anything else")
            internal fun `should evaluate to false on any other value`() {
                expectThat(enabledUnlessFalseOrEmpty).isFalse()
            }
        }

        @Nested
        inner class WithDisabledUnlessMatchesTrueOrEmpty {

            internal val disabledUnlessMatchesTrueOrEmpty by flag(defaultValue = false)

            @Test
            internal fun `should evaluate to false on absent value`() {
                expectThat(disabledUnlessMatchesTrueOrEmpty).isFalse()
            }

            @Test
            @SystemProperty(name = "disabledUnlessMatchesTrueOrEmpty", value = "true")
            internal fun `should evaluate to true on "true"`() {
                expectThat(disabledUnlessMatchesTrueOrEmpty).isTrue()
            }

            @Test
            @SystemProperty(name = "disabledUnlessMatchesTrueOrEmpty", value = "false")
            internal fun `should evaluate to false on "false"`() {
                expectThat(disabledUnlessMatchesTrueOrEmpty).isFalse()
            }

            @Test
            @SystemProperty(name = "disabledUnlessMatchesTrueOrEmpty", value = "")
            internal fun `should evaluate to true on empty value`() {
                expectThat(disabledUnlessMatchesTrueOrEmpty).isTrue()
            }

            @Test
            @SystemProperty(name = "disabledUnlessMatchesTrueOrEmpty", value = "anything else")
            internal fun `should evaluate to false on any other value`() {
                expectThat(disabledUnlessMatchesTrueOrEmpty).isFalse()
            }
        }
    }

    // TODO test or remove option
}
