package com.bkahlert.koodies.test.junit.e2e

import com.bkahlert.koodies.test.junit.SkipCondition
import com.bkahlert.koodies.test.junit.e2e.E2E.Companion.NAME
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith

/**
 * JUnit 5 annotation to explicitly denote [end-to-end tests](https://wiki.c2.com/?EndToEndPrinciple)).
 *
 * The more the "ends" of an end-to-end test reflect the actual system border (e.g. API request vs. user pressing a button in an app)
 * the more likely test execution is not always appropriate (e.g. because they take too much time) or
 * feasible (e.g. because of different architecture).
 *
 * Therefore **end-to-end test execution can be deactivated for a test run using system property `SystemProperties.skipE2ETests`**.
 *
 * Example: On the command line add `-DskipE2ETests` analogously to `-DskipTests`.
 *
 * Hint: Combining `skipTests` and `skipE2ETests` typically makes no sense as end-to-end tests
 * are only one kind of tests that consequently gets skipped already by the sole use of `skipTests`.
 * Instead you can use `skipUnitTests` which is complementary to `skipE2ETests`.
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
annotation class E2E {
    companion object {
        const val NAME = "E2E"
    }
}
