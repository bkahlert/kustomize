package com.imgcstmzr.util

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.cli.ColorHelpFormatter.Companion.tc
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan

class TestResultVisualizer : TestExecutionListener {
    private var failedTestsCount: Int = 0

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        super.executionFinished(testIdentifier, testExecutionResult)
        if (testExecutionResult.status == TestExecutionResult.Status.FAILED) failedTestsCount++
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        if (failedTestsCount == 0) {
            listOf(
                tc.bold("Done. All tests passed.")
            )
                .joinToString("\n")
                .border(padding = 2, margin = 1, ansiCode = tc.green)
                .border(padding = 0, margin = 2, ansiCode = tc.brightGreen)
                .also { echo(it) }
        } else {
            listOf(
                tc.bold("Done. $failedTestsCount tests failed!"),
            )
                .joinToString("\n")
                .border(padding = 2, margin = 1, ansiCode = tc.red)
                .border(padding = 0, margin = 2, ansiCode = tc.brightRed)
                .also { echo(it) }
        }
    }
}
