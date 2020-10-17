package com.bkahlert.koodies.test.junit

import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext

class SkipCondition : ExecutionCondition {

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult =
        with(context) {
            skippedTags.run {
                if (isEmpty()) enabled("$testName not skipped (default).")
                else disabled("$testName skipped because of $formatted")
            }
        }

    private val List<String>.numerus get() = if (size > 1) "tags " else ""
    private val List<String>.formatted get() = numerus + joinToString { "@Tag($it)" }

    private val ExtensionContext.testName get() :String = this.element.map { this.parent.map { it.testName }.orElse("") + " ➜ " + displayName }.orElse("/")
    private val ExtensionContext.skippedTags: List<String> get() = tags.filter { it.isSkipped() }

    private fun String.isSkipped(): Boolean {
        val propertyName = "skip${capitalize()}Tests"
        val propertyValue: String? = System.getProperty(propertyName)
        return propertyValue.isEmptyOrTrue
    }

    private val String?.isEmptyOrTrue get() = this?.let { it == "" || it.toBoolean() } ?: false
}
