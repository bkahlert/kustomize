package com.imgcstmzr

import com.bkahlert.koodies.boolean.asEmoji
import com.bkahlert.koodies.test.junit.E2E
import com.bkahlert.koodies.test.junit.JUnit.print
import com.bkahlert.koodies.test.junit.JUnit.runTests
import com.bkahlert.koodies.test.junit.Slow
import org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage
import org.junit.platform.launcher.TagFilter.excludeTags
import org.junit.platform.launcher.TagFilter.includeTags
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.listeners.LoggingListener
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary

object Tests {

    fun runTaggedTests(
        launcherDiscoveryRequestBuilder: LauncherDiscoveryRequestBuilder.() -> Unit,
    ): SummaryGeneratingListener =
        runTests(
            selectPackage("com.bkahlert.koodies"),
            selectPackage("com.imgcstmzr"),
            launcherConfigBuilder = { },
            launcherDiscoveryRequestBuilder = launcherDiscoveryRequestBuilder,
            launcher = { registerTestExecutionListeners(LoggingListener.forJavaUtilLogging()) }
        )
            .also { it.summary.print() }

    fun runFastUnitTests(): TestExecutionSummary = runTaggedTests { filters(excludeTags(Slow.NAME, E2E.NAME)) }.summary
    fun runSlowUnitTests(): TestExecutionSummary = runTaggedTests { filters(includeTags(Slow.NAME), excludeTags(E2E.NAME)) }.summary

private fun TestExecutionSummary.print() {
    printTo(PrintWriter(System.out))
}

fun main() {
    listOf(
        { Tests.runFastUnitTests() },
        { Tests.runSlowUnitTests() },
        { Tests.runE2ETests() })
        .takeWhile {
            it.invoke().failures.apply {
                if (this.isNotEmpty()) {
                    println("The following tests have failed:")
                    forEach { failure ->
                        println(false.asEmoji + " " + failure.testIdentifier.legacyReportingName)
                        println(failure.exception)
                        println("")
                    }
                }
            }.isEmpty()
        }
}
