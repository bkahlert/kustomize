package com.bkahlert.koodies.test.junit.unit

import com.bkahlert.koodies.test.junit.SkipCondition
import com.bkahlert.koodies.test.junit.unit.Unit.Companion.NAME
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith

/**
 * JUnit 5 annotation to explicitly denote [unit tests](https://wiki.c2.com/?UnitTest)).
 *
 * **Execution of unit tests can be deactivated for a test run using system property `SystemProperties.skipUnitTests`**.
 *
 * Example: On the command line add `-DskipUnitTests` analogously to `-DskipTests`.
 *
 * Hint: Combining `skipTests` and `skipUnitTests` typically makes no sense as unit tests
 * are only one kind of tests that consequently gets skipped already by the sole use of `skipTests`.
 * Instead you can use `skipIntegrationTests` which is complementary to `skipE2ETests`.
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
annotation class Unit {
    companion object {
        const val NAME = "Unit"
    }
}
