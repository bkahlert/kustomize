package com.imgcstmzr.util.debug

import com.bkahlert.koodies.test.junit.hasDebugSiblings
import com.bkahlert.koodies.test.junit.isDebug
import com.bkahlert.koodies.test.junit.testName
import com.imgcstmzr.util.quoted
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext

class NoDebugSiblingsCondition : ExecutionCondition {
    fun ExtensionContext.getEnabledDueToAbsentDebugAnnotation() =
        enabled("Neither ${testName.quoted} nor any other test is annotated with @${Debug::class.simpleName}.")

    fun ExtensionContext.getEnabledDueToDebugAnnotation() =
        enabled("Test ${testName.quoted} is annotated with @${Debug::class.simpleName}.")

    fun ExtensionContext.getDisabledDueToSiblingDebugAnnotation() =
        disabled("Test ${testName.quoted} skipped due to existing @${Debug::class.simpleName} annotation on another test.")

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult =
        when (context.hasDebugSiblings) {
            true -> when (context.isDebug) {
                true -> context.getEnabledDueToDebugAnnotation()
                else -> context.getDisabledDueToSiblingDebugAnnotation()
            }
            else -> context.getEnabledDueToAbsentDebugAnnotation()
        }
}
