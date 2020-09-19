package com.imgcstmzr.util

import com.imgcstmzr.process.Exec
import org.junit.jupiter.api.extension.ConditionEvaluationResult
import org.junit.jupiter.api.extension.ConditionEvaluationResult.disabled
import org.junit.jupiter.api.extension.ConditionEvaluationResult.enabled
import org.junit.jupiter.api.extension.ExecutionCondition
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.platform.commons.support.AnnotationSupport
import java.lang.reflect.AnnotatedElement
import java.util.Optional

/**
 * Declares a requirement on Docker.
 * If no Docker is available this test is skipped.
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class DockerRequired

class DockerRequiredCondition : ExecutionCondition {
    val enabledCondition by lazy { enabled("No ${DockerRequired::class.simpleName} annotation found.") }
    val disabledCondition by lazy {
        val text = "Test ist annotated with ${DockerRequired::class.simpleName} but no Docker is running."
        disabled(text.border(padding = 2, margin = 1))
    }
    val dockerUpAndRunning by lazy { runCatching { Exec.Sync.execCommand("docker", "info") }.isSuccess }

    override fun evaluateExecutionCondition(context: ExtensionContext): ConditionEvaluationResult {
        return if (context.element { isA<DockerRequired>() } && !dockerUpAndRunning) disabledCondition
        else enabledCondition
    }
}

/**
 * Checks if the test class, method, test template, etc. of the current scope fulfills the requirements implemented by the provided [tester].
 */
fun ExtensionContext.element(tester: AnnotatedElement.() -> Boolean) = element.map(tester).orElse(false)

/**
 * Checks if at least one of [this] elements [annotations] or meta annotations is of the provided type.
 */
inline fun <reified T : Annotation> AnnotatedElement?.isA(): Boolean =
    AnnotationSupport.isAnnotated(Optional.ofNullable(this), T::class.java)
