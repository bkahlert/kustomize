package com.bkahlert.koodies.test.junit

import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit.MINUTES

/**
 * JUnit 5 annotation to denote slow that, which may take up to 30 minutes.
 */
@Timeout(30, unit = MINUTES)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class ThirtyMinutesTimeout