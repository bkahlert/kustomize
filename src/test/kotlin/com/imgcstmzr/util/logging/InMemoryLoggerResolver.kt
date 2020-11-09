package com.imgcstmzr.util.logging

import com.bkahlert.koodies.test.junit.Verbosity.Companion.isVerbose
import com.bkahlert.koodies.test.junit.testName
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.create
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class InMemoryLoggerResolver : ParameterResolver, AfterEachCallback {

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
        object : InMemoryLogger<Unit>(
            caption = testName + if (suffix != null) "::$suffix" else "",
            borderedOutput = borderedOutput,
            statusInformationColumn = parameterContext.findAnnotation(Columns::class.java).map { it.value }.orElse(-1),
            outputStreams = if (isVerbose || parameterContext.isVerbose) listOf(System.out) else emptyList(),
        ) {
            private var resultLogged = false
            override fun logResult(block: () -> Result<Unit>) {
                if (!resultLogged) {
                    super.logResult(block)
                    resultLogged = true
                }
            }
        }.also { store().put(element, it) }

    override fun afterEach(extensionContext: ExtensionContext) {
        val logger: InMemoryLogger<*>? = extensionContext.store().get(extensionContext.element, InMemoryLogger::class.java)
        if (logger != null) {
            val result = extensionContext.executionException.map { Result.failure<Unit>(it) }.orElseGet { Result.success(Unit) }
            if (result.exceptionOrNull() is AssertionError) return
            kotlin.runCatching {
                @Suppress("UNCHECKED_CAST")
                (logger.logResult { result as Result<Nothing> })
            }
        }
    }

    private fun ExtensionContext.store(): ExtensionContext.Store = getStore(create(InMemoryLoggerResolver::class.java))
}
