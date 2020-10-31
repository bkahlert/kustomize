package com.bkahlert.koodies.test.junit

import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener

object JUnit {
    fun runTests(vararg selectors: DiscoverySelector): SummaryGeneratingListener =
        SummaryGeneratingListener().also { listener ->
            LauncherDiscoveryRequestBuilder.request().selectors(*selectors).build().also { request ->
                LauncherFactory.create().apply {
                    discover(request)
                    registerTestExecutionListeners(listener)
                    execute(request)
                }
            }
        }
}
