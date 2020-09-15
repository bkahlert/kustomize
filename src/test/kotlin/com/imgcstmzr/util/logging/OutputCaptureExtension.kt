package com.imgcstmzr.util.logging

import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class OutputCaptureExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback, AfterEachCallback, ParameterResolver {

    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext?): Boolean =
        CapturedOutput::class.java == parameterContext.parameter.type

    override fun resolveParameter(parameterContext: ParameterContext?, extensionContext: ExtensionContext): Any? =
        getOutputCapture(extensionContext)

    private fun getOutputCapture(context: ExtensionContext): OutputCapture =
        getStore(context).getOrComputeIfAbsent(OutputCapture::class.java)

    private fun getStore(context: ExtensionContext): ExtensionContext.Store =
        context.getStore(ExtensionContext.Namespace.create(javaClass))

    override fun beforeAll(context: ExtensionContext) {
        getOutputCapture(context).push()
    }

    override fun afterAll(context: ExtensionContext) {
        getOutputCapture(context).pop()
    }

    override fun beforeEach(context: ExtensionContext) {
        getOutputCapture(context).push()
    }

    override fun afterEach(context: ExtensionContext) {
        getOutputCapture(context).pop()
    }
}
