package com.bkahlert.koodies.test.junit

import com.bkahlert.koodies.docker.DockerContainerName
import com.bkahlert.koodies.docker.DockerContainerName.Companion.toContainerName
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

class DockerContainerNameResolver : ParameterResolver {
    override fun supportsParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Boolean = parameterContext.parameter.type.let {
        when {
            DockerContainerName::class.java.isAssignableFrom(it) -> true
            else -> false
        }
    }

    @Suppress("RedundantNullableReturnType")
    override fun resolveParameter(parameterContext: ParameterContext, extensionContext: ExtensionContext): Any? = parameterContext.parameter.type.let {
        when {
            DockerContainerNameResolver::class.java.isAssignableFrom(it) -> extensionContext.testName.toContainerName()
            else -> error("Unsupported $parameterContext")
        }
    }
}
