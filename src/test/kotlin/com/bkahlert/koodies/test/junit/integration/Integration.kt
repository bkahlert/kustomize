package com.bkahlert.koodies.test.junit.integration

import com.bkahlert.koodies.test.junit.SkipCondition
import com.bkahlert.koodies.test.junit.integration.Integration.Companion.NAME
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith

/**
 * JUnit 5 annotation to explicitly denote [integration tests](https://wiki.c2.com/?IntegrationTest)).
 *
 * **Execution of integration tests can be deactivated for a test run using system property `SystemProperties.skipIntegrationTests`**.
 *
 * Example: On the command line add `-DskipIntegrationTests` analogously to `-DskipTests` or `-DskipE2ETests`.
 *
 * Hint: Combining `skipTests` and `skipIntegrationTests` typically makes no sense as integration tests
 * are only one kind of tests that consequently gets skipped already by the sole use of `skipTests`.
 * Instead you can use `skipUnitTests` and `skipE2ETests` which cover all tests but the integration tests.
 */
@ExtendWith(SkipCondition::class)
@Tag(NAME)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class Integration {
    companion object {
        const val NAME = "Integration"
    }
}
