package com.imgcstmzr.util

import com.bkahlert.koodies.docker.Docker
import com.bkahlert.koodies.terminal.ascii.Boxes.Companion.wrapWithBox
import com.bkahlert.koodies.test.junit.debug.Debug
import com.bkahlert.koodies.test.junit.isAnnotated
import com.bkahlert.koodies.test.junit.testName
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext

class DockerRequiredCondition : ExecutionCondition {
    fun ExtensionContext.getEnabledDueToAbsentDebugAnnotation() =
        enabled("Neither ${testName.quoted} nor any other test is annotated with @${Debug::class.simpleName}.")

    val dockerUpAndRunning get() = Docker.isEngineRunning
    val annotation = "@${DockerRequired::class.simpleName}"
    fun ExtensionContext.getEnabledCondition() = enabled("No $annotation annotation at test ${testName.quoted} found.")
    fun ExtensionContext.getDisabledCondition() = disabled("Test ${testName.quoted} is annotated with $annotation but no Docker is running.".wrapWithBox())

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        return if (context.isAnnotated<DockerRequired>() && !dockerUpAndRunning) context.getDisabledCondition()
        else context.getEnabledCondition()
    }
}


