@file:Suppress("unused")

package com.bkahlert.koodies.test.junit

import com.bkahlert.koodies.test.junit.e2e.E2E
import com.bkahlert.koodies.test.junit.integration.Integration
import com.bkahlert.koodies.test.junit.systemproperties.SystemProperties
import com.bkahlert.koodies.test.junit.systemproperties.SystemProperty
import com.bkahlert.koodies.test.junit.unit.Unit
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.containsExactlyInAnyOrder

/**
 * Tests `skipUnitTests`, `skipIntegrationTests` and `skipE2ETests`.
 *
 * Since it can unintentionally deactivate actual tests, this test container runs [Isolated].
 */
@Isolated
@Execution(SAME_THREAD)
internal class SkipConditionIntTest {

    @Nested
    @SystemProperties(
        SystemProperty(name = "skipUnitTests", value = "true"),
        SystemProperty(name = "skipIntegrationTests", value = "true"),
        SystemProperty(name = "skipE2ETests", value = "true")
    )
    inner class AllTestTypesSkipped {
        val runMethods = mutableListOf<Function<*>>()

        @Test
        internal fun `untyped test should run`() {
            runMethods.add(::`untyped test should run`)
        }

        @Unit
        @Test
        internal fun `unit test should not run`() {
            expectThat(::`unit test should not run`).get { fail("$name did run") }
        }

        @Integration
        @Test
        internal fun `integration test should not run`() {
            expectThat(::`integration test should not run`).get { fail("$name did run") }
        }

        @E2E
        @Test
        internal fun `e2e test should not run`() {
            expectThat(::`e2e test should not run`).get { fail("$name did run") }
        }

        @AfterAll
        internal fun assertRunMethods() {
            expectThat(runMethods).containsExactlyInAnyOrder(
                ::`untyped test should run`
            )
        }
    }

    @Nested
    @SystemProperties(
        SystemProperty(name = "skipUnitTests", value = "true"),
        SystemProperty(name = "skipIntegrationTests", value = "false"),
        SystemProperty(name = "skipE2ETests", value = "false")
    )
    inner class UnitTestsSkipped {
        val runMethods = mutableListOf<Function<*>>()

        @Test
        internal fun `untyped test should run`() {
            runMethods.add(::`untyped test should run`)
        }

        @Unit
        @Test
        internal fun `unit test should not run`() {
            expectThat(::`unit test should not run`).get { fail("$name did run") }
        }

        @Integration
        @Test
        internal fun `integration test should run`() {
            runMethods.add(::`integration test should run`)
        }

        @E2E
        @Test
        internal fun `e2e test should run`() {
            runMethods.add(::`e2e test should run`)
        }

        @AfterAll
        internal fun assertRunMethods() {
            expectThat(runMethods).containsExactlyInAnyOrder(
                ::`untyped test should run`,
                ::`integration test should run`,
                ::`e2e test should run`
            )
        }
    }

    @Nested
    @SystemProperties(
        SystemProperty(name = "skipUnitTests", value = "false"),
        SystemProperty(name = "skipIntegrationTests", value = "true"),
        SystemProperty(name = "skipE2ETests", value = "false")
    )
    inner class IntegrationTestsSkipped {
        val runMethods = mutableListOf<Function<*>>()

        @Test
        internal fun `untyped test should run`() {
            runMethods.add(::`untyped test should run`)
        }

        @Unit
        @Test
        internal fun `unit test should run`() {
            runMethods.add(::`unit test should run`)
        }

        @Integration
        @Test
        internal fun `integration test should not run`() {
            expectThat(::`integration test should not run`).get { fail("$name did run") }
        }

        @E2E
        @Test
        internal fun `e2e test should run`() {
            runMethods.add(::`e2e test should run`)
        }

        @AfterAll
        internal fun assertRunMethods() {
            expectThat(runMethods).containsExactlyInAnyOrder(
                ::`untyped test should run`,
                ::`unit test should run`,
                ::`e2e test should run`
            )
        }
    }

    @Nested
    @SystemProperties(
        SystemProperty(name = "skipUnitTests", value = "false"),
        SystemProperty(name = "skipIntegrationTests", value = "false"),
        SystemProperty(name = "skipE2ETests", value = "true")
    )
    inner class E2ETestsSkipped {
        val runMethods = mutableListOf<Function<*>>()

        @Test
        internal fun `untyped test should run`() {
            runMethods.add(::`untyped test should run`)
        }

        @Unit
        @Test
        internal fun `unit test should run`() {
            runMethods.add(::`unit test should run`)
        }

        @Integration
        @Test
        internal fun `integration test should run`() {
            runMethods.add(::`integration test should run`)
        }

        @E2E
        @Test
        internal fun `e2e test should not run`() {
            expectThat(::`e2e test should not run`).get { fail("$name did run") }
        }

        @AfterAll
        internal fun assertRunMethods() {
            expectThat(runMethods).containsExactlyInAnyOrder(
                ::`untyped test should run`,
                ::`unit test should run`,
                ::`integration test should run`
            )
        }
    }

    /**
     * Integration tests `skipUnitTests`, `skipIntegrationTests` and `skipE2ETests`.
     *
     * This test container is disabled if either one of the properties is already present
     * to not break the GitLab pipeline if that is an actually used property.
     */
    @Nested
    @SystemProperties(SystemProperty(name = "skipSomethingElse", value = "true"))
    @Unit @Integration @E2E
    inner class NothingSkipped {
        val runMethods = mutableListOf<Function<*>>()

        @Test
        internal fun `untyped test should run`() {
            runMethods.add(::`untyped test should run`)
        }

        @Unit
        @Test
        internal fun `unit test should run`() {
            runMethods.add(::`unit test should run`)
        }

        @Integration
        @Test
        internal fun `integration test should run`() {
            runMethods.add(::`integration test should run`)
        }

        @E2E
        @Test
        internal fun `e2e test should run`() {
            runMethods.add(::`e2e test should run`)
        }

        @AfterAll
        internal fun assertRunMethods() {
            expectThat(runMethods).containsExactlyInAnyOrder(
                ::`untyped test should run`,
                ::`unit test should run`,
                ::`integration test should run`,
                ::`e2e test should run`
            )
        }
    }
}
