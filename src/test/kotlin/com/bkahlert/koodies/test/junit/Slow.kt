package com.bkahlert.koodies.test.junit

import com.bkahlert.koodies.test.junit.Slow.Companion.NAME
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit.MINUTES

/**
 * JUnit 5 annotation to denote slow that, which may take up to 2 minutes.
 */
@Timeout(2, unit = MINUTES)
@Tag(NAME)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class Slow {
    companion object {
        const val NAME = "Slow"
    }
}
