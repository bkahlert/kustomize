package com.imgcstmzr.util.debug

import com.bkahlert.koodies.test.junit.hasDebugSiblings
import com.bkahlert.koodies.test.junit.isDebug
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext

class NoDebugSiblingsCondition : ExecutionCondition {
    val enabledDueToAbsentDebugAnnotation by lazy { enabled("No ${Debug::class.simpleName} annotation found.") }
    val enabledDueToDebugAnnotation by lazy { enabled("Annotated with ${Debug::class.simpleName}.") }
    val disabledDueToSiblingDebugAnnotation by lazy { disabled("Sibling ${Debug::class.simpleName} annotated tests found.") }

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult =
        when (context.hasDebugSiblings) {
            true -> when (context.isDebug) {
                true -> enabledDueToDebugAnnotation
                else -> disabledDueToSiblingDebugAnnotation
            }
            else -> enabledDueToAbsentDebugAnnotation
        }
}
