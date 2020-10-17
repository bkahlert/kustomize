package com.bkahlert.koodies.test.junit

import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode

@TestFactory @Execution(ExecutionMode.CONCURRENT)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
annotation class ConcurrentTestFactory 