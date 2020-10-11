package com.imgcstmzr.util.logging

import com.bkahlert.koodies.test.junit.allTests
import com.bkahlert.koodies.test.junit.isA
import com.bkahlert.koodies.test.junit.isDebug
import com.bkahlert.koodies.test.junit.uniqueName
import com.imgcstmzr.util.debug.Debug
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.create
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestPlan

class InMemoryLoggerResolver : ParameterResolver, AfterEachCallback, TestExecutionListener {

    companion object {
        // Hack but wouldn't know how else to get the number of tests
        // in order to activate logging.
        var testCount: Int? = null
    }

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        testCount = testPlan.allTests.size
    }

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean = parameterContext.parameter.type.let {
        when {
            InMemoryLogger::class.java.isAssignableFrom(it) -> true
            InMemoryLoggerFactory::class.java.isAssignableFrom(it) -> true
            else -> false
        }
    }

    @Suppress("RedundantNullableReturnType")
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? = parameterContext.parameter.type.let {
        when {
            InMemoryLogger::class.java.isAssignableFrom(it) -> extensionContext.createLogger(borderedOutput = true, parameterContext = parameterContext)
            InMemoryLoggerFactory::class.java.isAssignableFrom(it) -> object : InMemoryLoggerFactory<Unit> {
                override fun createLogger(customSuffix: String, borderedOutput: Boolean): InMemoryLogger<Unit> =
                    extensionContext.createLogger(customSuffix, borderedOutput, parameterContext)
            }
            else -> error("Unsupported $parameterContext")
        }
    }

    private fun ExtensionContext.createLogger(suffix: String? = null, borderedOutput: Boolean, parameterContext: ParameterContext): InMemoryLogger<Unit> =
        InMemoryLogger<Unit>(
            caption = uniqueName + if (suffix != null) "::$suffix" else "",
            borderedOutput = borderedOutput,
            outputStreams = if (isVerbose || parameterContext.isVerbose) listOf(System.out) else emptyList()
        ).also { store().put(element, it) }

    override fun afterEach(extensionContext: ExtensionContext) {
        val logger: InMemoryLogger<*>? = extensionContext.store().get(extensionContext.element, InMemoryLogger::class.java)
        if (logger != null) {
            val result = extensionContext.executionException.map { Result.failure<Unit>(it) }.orElseGet { Result.success(Unit) }
            if (result.exceptionOrNull() is AssertionError) return
            kotlin.runCatching {
                @Suppress("UNCHECKED_CAST")
                logger.logLast(result as Result<Nothing>)
            }
        }
    }

    private fun ExtensionContext.store(): ExtensionContext.Store = getStore(create(InMemoryLoggerResolver::class.java))
    private val ExtensionContext.isVerbose get() = isDebug || testCount == 1
    private val ParameterContext.isVerbose get() = this.parameter.isA<Debug>()
}