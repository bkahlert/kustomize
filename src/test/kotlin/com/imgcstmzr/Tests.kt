package com.imgcstmzr

import com.bkahlert.koodies.boolean.asEmoji
import com.bkahlert.koodies.string.LineSeparators.LF
import com.bkahlert.koodies.terminal.ansi.AnsiColors.green
import com.bkahlert.koodies.terminal.ascii.Borders
import com.bkahlert.koodies.terminal.ascii.Boxes
import com.bkahlert.koodies.terminal.ascii.Boxes.Companion.wrapWithBox
import com.bkahlert.koodies.terminal.ascii.Draw.Companion.draw
import com.bkahlert.koodies.terminal.ascii.wrapWithBorder
import com.bkahlert.koodies.test.junit.JUnit.render
import com.bkahlert.koodies.test.junit.JUnit.runTests
import com.bkahlert.koodies.test.junit.Slow
import org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage
import org.junit.platform.launcher.TagFilter.excludeTags
import org.junit.platform.launcher.TagFilter.includeTags
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.listeners.LoggingListener
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary
import kotlin.system.exitProcess

object Tests {

    private fun runTaggedTests(
        name: String,
        launcherDiscoveryRequestBuilder: LauncherDiscoveryRequestBuilder.() -> Unit,
    ): SummaryGeneratingListener {
        val verticalSpace = LF.repeat(5)
        println(verticalSpace + name.wrapWithBox(Boxes.SPHERICAL).wrapWithBorder(Borders.Rounded, padding = 10, margin = 15).green() + verticalSpace)
        return runTests(
            selectPackage("com.bkahlert.koodies"),
            selectPackage("com.imgcstmzr"),
            launcherConfigBuilder = { },
            launcherDiscoveryRequestBuilder = launcherDiscoveryRequestBuilder,
            launcher = { registerTestExecutionListeners(LoggingListener.forJavaUtilLogging()) }
        )
            .also {
                val summary = "Detailed Summary".toUpperCase() + LF + it.summary.render()
                println(summary.draw.border.heavyDotted(padding = 2, margin = 1).replace("[", " ").replace("]", " "))
            }
    }

    fun runFastUnitTests(): TestExecutionSummary = runTaggedTests("Fast Unit Tests") { filters(excludeTags(Slow.NAME, E2E.NAME)) }.summary
    fun runSlowUnitTests(): TestExecutionSummary = runTaggedTests("Slow Unit Tests") { filters(includeTags(Slow.NAME), excludeTags(E2E.NAME)) }.summary
    fun runE2ETests(): TestExecutionSummary = runTaggedTests("End-to-End Tests") { filters(includeTags(E2E.NAME)) }.summary
}

fun main() {
    listOf(
        { Tests.runFastUnitTests() },
        { Tests.runSlowUnitTests() },
        { Tests.runE2ETests() },
    ).takeWhile {
        it.invoke().failures.apply {
            if (this.isNotEmpty()) {
                println("The following tests have failed:")
                forEach { failure ->
                    println(false.asEmoji + " " + failure.testIdentifier.legacyReportingName)
                    println(failure.exception)
                    println("")
                }
                exitProcess(-1)
            }
        }.isEmpty()
    }
}
