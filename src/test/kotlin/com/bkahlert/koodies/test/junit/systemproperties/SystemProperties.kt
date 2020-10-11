package com.bkahlert.koodies.test.junit.systemproperties

/**
 * Allows to annotate [SystemProperty] multiple times.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.VALUE_PARAMETER
)
annotation class SystemProperties(vararg val value: SystemProperty)
