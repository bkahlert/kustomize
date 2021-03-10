package com.imgcstmzr.test.logging

import com.imgcstmzr.test.Verbosity.Companion.isVerbose
import com.imgcstmzr.test.testName
import koodies.logging.InMemoryLogger
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ExtensionContext.Namespace.create
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class InMemoryLoggerResolver : ParameterResolver, AfterEachCallback {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean =
        parameterContext.parameter.type.let {
            when {
                InMemoryLogger::class.java.isAssignableFrom(it) -> true
                InMemoryLoggerFactory::class.java.isAssignableFrom(it) -> true
                else -> false
            }
        }

    @Suppress("RedundantNullableReturnType")
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? =
        parameterContext.parameter.type.let {
            when {
                InMemoryLogger::class.java.isAssignableFrom(it) -> extensionContext.createLogger(
                    bordered = true,
                    parameterContext = parameterContext
                )
                InMemoryLoggerFactory::class.java.isAssignableFrom(it) -> object : InMemoryLoggerFactory {
                    override fun createLogger(customSuffix: String, bordered: Boolean): InMemoryLogger =
                        extensionContext.createLogger(customSuffix, bordered, parameterContext)
                }
                else -> error("Unsupported $parameterContext")
            }
        }

    private fun ExtensionContext.createLogger(
        suffix: String? = null,
        bordered: Boolean,
        parameterContext: ParameterContext,
    ): InMemoryLogger =
        object : InMemoryLogger(
            caption = testName + if (suffix != null) "::$suffix" else "",
            bordered = bordered,
            statusInformationColumn = parameterContext.findAnnotation(Columns::class.java).map { it.value }.orElse(-1),
            outputStreams = if (isVerbose || parameterContext.isVerbose) listOf(System.out) else emptyList(),
        ) {
            override fun <R> logResult(block: () -> Result<R>): R {
                @Suppress("UNCHECKED_CAST")
                return if (!resultLogged) {
                    super.logResult(block).also { resultLogged = true }
                } else Unit as R
            }
        }.save(this)

    override fun afterEach(extensionContext: ExtensionContext) {
        val logger: InMemoryLogger? = extensionContext.store().get(extensionContext.element, InMemoryLogger::class.java)
        if (logger != null) {
            val result =
                extensionContext.executionException.map { Result.failure<Any>(it) }.orElseGet { Result.success(Unit) }
            if (result.exceptionOrNull() is AssertionError) return
            kotlin.runCatching {
                logger.logResult { result }
            }
        }
    }
}

private fun ExtensionContext.store(): ExtensionContext.Store = getStore(create(InMemoryLoggerResolver::class.java))
private fun InMemoryLogger.save(extensionContext: ExtensionContext): InMemoryLogger =
    also { extensionContext.store().put(extensionContext.element, this) }

fun ExtensionContext.logger(): InMemoryLogger? = store().get(element, InMemoryLogger::class.java)