package com.bkahlert.koodies.test.junit

import org.junit.platform.engine.DiscoverySelector
import org.junit.platform.launcher.Launcher
import org.junit.platform.launcher.core.LauncherConfig
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.core.LauncherFactory
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary
import java.io.PrintWriter

object JUnit {

    fun runTests(
        vararg selectors: DiscoverySelector,
        launcherDiscoveryRequestBuilder: (LauncherDiscoveryRequestBuilder.() -> Unit)? = null,
        launcherConfigBuilder: (LauncherConfig.Builder.() -> Unit)? = null,
        launcher: (Launcher.() -> Unit)? = null,
    ): SummaryGeneratingListener =
        SummaryGeneratingListener().also { listener ->
            LauncherDiscoveryRequestBuilder
                .request()
                .selectors(*selectors)
                .apply(launcherDiscoveryRequestBuilder ?: {})
                .build().also { request ->
                    val launcherConfig = LauncherConfig.builder().apply(launcherConfigBuilder ?: {}).build()
                    LauncherFactory.create(launcherConfig).apply {
                        discover(request)
                        registerTestExecutionListeners(listener)
                        apply(launcher ?: {})
                        execute(request)
                    }
                }
        }

    fun TestExecutionSummary.print() {
        printTo(PrintWriter(System.out))
    }
}
