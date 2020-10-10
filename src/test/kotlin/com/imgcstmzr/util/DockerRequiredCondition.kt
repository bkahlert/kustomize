package com.imgcstmzr.util

import com.bkahlert.koodies.docker.Docker
import com.bkahlert.koodies.terminal.ascii.wrapWithBorder
import com.bkahlert.koodies.test.junit.isAnnotated
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext

class DockerRequiredCondition : ExecutionCondition {
    val annotation = "@${DockerRequired::class.simpleName}"
    val enabledCondition by lazy { enabled("No $annotation annotation found.") }
    val disabledCondition by lazy { disabled("Test is annotated with $annotation but no Docker is running.".wrapWithBorder()) }
    val dockerUpAndRunning by lazy { Docker.isRunning }

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        return if (context.isAnnotated<DockerRequired>() && !dockerUpAndRunning) disabledCondition
        else enabledCondition
    }
}


