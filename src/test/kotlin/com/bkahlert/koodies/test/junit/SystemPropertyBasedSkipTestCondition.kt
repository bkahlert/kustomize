package com.bkahlert.koodies.test.junit

import com.imgcstmzr.util.quoted
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.ExtensionContext

/**
 * [ExecutionCondition] for [@SkipIfSystemPropertyIsTrueOrEmpty][SkipIfSystemPropertyIsTrueOrEmpty].
 */
class SystemPropertyBasedSkipTestCondition :
    ExecutionCondition { // TODO test, split maybe into @Tag evaluating extension; currently allws  @Tag("Unit") and @SkipIfSystemPropertyIsTrueOrEmpty("skipUnitTests")
    private fun skippedReason(skippedTags: List<String>): String {
        // TODO add class + method
        return if (skippedTags.isNotEmpty()) "Skipped because of ${if (skippedTags.size > 1) "tags " else ""} ${skippedTags.joinToString { "@Tag(${it.quoted})" }}"
        else "Skipping."
    }

    private val notSkippedReason: String = "Skipping disabled by default."

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        val enabledSkippingBySystemProperty = context.isAnnotated<SkipIfSystemPropertyIsTrueOrEmpty>() { it.isSkippingEnabled }
        val skippedTags = context.tags.mapNotNull { type ->
            val propertyName = "skip${type}Tests"
            val propertyValue = System.getProperty(propertyName)
            val booleanValue = propertyValue?.let { it == "" || it == "true" } ?: false
            if (booleanValue) type else null
        }
        return if (enabledSkippingBySystemProperty || skippedTags.isNotEmpty()) ConditionEvaluationResult.disabled(skippedReason(skippedTags))
        else ConditionEvaluationResult.enabled(notSkippedReason)
    }
}

@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@ExtendWith(SystemPropertyBasedSkipTestCondition::class)
annotation class SkipIfAtLeastOneSystemPropertyIsEnabled(val systemProperties: Array<SkipIfSystemPropertyIsTrueOrEmpty>)

@Suppress("DEPRECATED_JAVA_ANNOTATION") // needed to use multiple times
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER)
@Retention(AnnotationRetention.RUNTIME)
@Repeatable
@java.lang.annotation.Repeatable(SkipIfAtLeastOneSystemPropertyIsEnabled::class)
@MustBeDocumented
@ExtendWith(SystemPropertyBasedSkipTestCondition::class)
annotation class SkipIfSystemPropertyIsTrueOrEmpty(val value: String)

val SkipIfSystemPropertyIsTrueOrEmpty.systemPropertyName: String get() = "skip${value}Tests"
val SkipIfSystemPropertyIsTrueOrEmpty.isSkippingEnabled: Boolean by SystemProperty.flag(false)
val SkipIfSystemPropertyIsTrueOrEmpty.reason: String get() = "$systemPropertyName is ${if (isSkippingEnabled) "enabled" else "disabled"}"
