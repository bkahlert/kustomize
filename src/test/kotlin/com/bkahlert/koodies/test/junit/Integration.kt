package com.bkahlert.koodies.test.junit

import com.bkahlert.koodies.test.junit.E2E.Companion.NAME
import org.junit.jupiter.api.Tag

/**
 * JUnit 5 annotation to explicitly denote [end-to-end tests](https://wiki.c2.com/?IntegrationTest).
 */
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
