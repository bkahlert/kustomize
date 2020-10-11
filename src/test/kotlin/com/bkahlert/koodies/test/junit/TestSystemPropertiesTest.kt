package com.bkahlert.koodies.test.junit

import com.bkahlert.koodies.test.junit.e2e.E2ETest
import com.bkahlert.koodies.test.junit.integration.IntegrationTest
import com.bkahlert.koodies.test.junit.systemproperties.SystemProperty
import com.bkahlert.koodies.test.junit.unit.UnitTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.isFalse
import strikt.assertions.isTrue

@Execution(ExecutionMode.CONCURRENT)
internal class TestSystemPropertiesTest {

    /**
     * Tests [TestSystemProperties.skipUnitTests].
     *
     * Since it can unintentionally deactivate actual tests, this test container runs [Isolated].
     *
     * This test container is disabled if [TestSystemProperties.skipUnitTests] is already present
     * to not break the GitLab pipeline if that is an actually used property.
     */
    @Isolated
    @Nested
    @SkipIfSystemPropertyIsTrueOrEmpty(UnitTest.NAME)
    inner class SkipUnitTests {

        @Test
        internal fun `should evaluate to false on absent value (= run if not specified)`() {
            expectThat(TestSystemProperties.skipUnitTests).isFalse()
        }

        @Test
        @SystemProperty(name = "skipUnitTests", value = "true")
        internal fun `should evaluate to true on "true" (= skip if set to skip)`() {
            expectThat(TestSystemProperties.skipUnitTests).isTrue()
        }

        @Test
        @SystemProperty(name = "skipUnitTests", value = "false")
        internal fun `should evaluate to false on "false" (= run if set to not skip)`() {
            expectThat(TestSystemProperties.skipUnitTests).isFalse()
        }

        @Test
        @SystemProperty(name = "skipUnitTests", value = "")
        internal fun `should evaluate to true on empty (= skip if used as a toggle)`() {
            expectThat(TestSystemProperties.skipUnitTests).isTrue()
        }

        @Test
        @SystemProperty(name = "skipUnitTests", value = "incorrect value")
        internal fun `should evaluate to false on any other value (= incorrect, thus not specified, thus run)`() {
            expectThat(TestSystemProperties.skipUnitTests).isFalse()
        }
    }

    /**
     * Tests [TestSystemProperties.skipIntegrationTests].
     *
     * Since it can unintentionally deactivate actual tests, this test container runs [Isolated].
     *
     * This test container is disabled if [TestSystemProperties.skipIntegrationTests] is already present
     * to not break the GitLab pipeline if that is an actually used property.
     */
    @Isolated
    @Nested
    @SkipIfSystemPropertyIsTrueOrEmpty(IntegrationTest.NAME)
    inner class SkipIntegrationTests {

        @Test
        internal fun `should evaluate to false on absent value (= run if not specified)`() {
            expectThat(TestSystemProperties.skipIntegrationTests).isFalse()
        }

        @Test
        @SystemProperty(name = "skipIntegrationTests", value = "true")
        internal fun `should evaluate to true on "true" (= skip if set to skip)`() {
            expectThat(TestSystemProperties.skipIntegrationTests).isTrue()
        }

        @Test
        @SystemProperty(name = "skipIntegrationTests", value = "false")
        internal fun `should evaluate to false on "false" (= run if set to not skip)`() {
            expectThat(TestSystemProperties.skipIntegrationTests).isFalse()
        }

        @Test
        @SystemProperty(name = "skipIntegrationTests", value = "")
        internal fun `should evaluate to true on empty (= skip if used as a toggle)`() {
            expectThat(TestSystemProperties.skipIntegrationTests).isTrue()
        }

        @Test
        @SystemProperty(name = "skipIntegrationTests", value = "anything else")
        internal fun `should evaluate to false on any value (= incorrect, thus not specified, thus run)`() {
            expectThat(TestSystemProperties.skipIntegrationTests).isFalse()
        }
    }

    /**
     * Tests [TestSystemProperties.skipE2ETests].
     *
     * Since it can unintentionally deactivate actual tests, this test container runs [Isolated].
     *
     * This test container is disabled if [TestSystemProperties.skipE2ETests] is already present
     * to not break the GitLab pipeline if that is an actually used property.
     */
    @Isolated
    @Nested
    @SkipIfSystemPropertyIsTrueOrEmpty(E2ETest.NAME)
    inner class SkipE2ETests {

        @Test
        internal fun `should evaluate to false on absent value (= run if not specified)`() {
            expectThat(TestSystemProperties.skipE2ETests).isFalse()
        }

        @Test
        @SystemProperty(name = "skipE2ETests", value = "true")
        internal fun `should evaluate to true on "true" (= skip if set to skip)`() {
            expectThat(TestSystemProperties.skipE2ETests).isTrue()
        }

        @Test
        @SystemProperty(name = "skipE2ETests", value = "false")
        internal fun `should evaluate to false on "false" (= run if set to not skip)`() {
            expectThat(TestSystemProperties.skipE2ETests).isFalse()
        }

        @Test
        @SystemProperty(name = "skipE2ETests", value = "")
        internal fun `should evaluate to true on empty (= skip if used as a toggle)`() {
            expectThat(TestSystemProperties.skipE2ETests).isTrue()
        }

        @Test
        @SystemProperty(name = "skipE2ETests", value = "anything else")
        internal fun `should evaluate to false on any value (= incorrect, thus not specified, thus run)`() {
            expectThat(TestSystemProperties.skipE2ETests).isFalse()
        }
    }
}
