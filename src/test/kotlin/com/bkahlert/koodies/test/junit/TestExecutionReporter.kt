package com.bkahlert.koodies.test.junit

import com.bkahlert.koodies.terminal.ANSI
import com.bkahlert.koodies.terminal.ascii.Draw.Companion.draw
import com.bkahlert.koodies.terminal.ascii.wrapWithBorder
import com.bkahlert.koodies.test.junit.debug.Debug
import com.github.ajalt.clikt.output.TermUi.echo
import org.junit.jupiter.api.extension.TestWatcher
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.lang.reflect.AnnotatedElement
import kotlin.properties.Delegates
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds

@ExperimentalTime
class TestExecutionReporter : TestExecutionListener, TestWatcher {

    private var startTimestamp by Delegates.notNull<Long>()
    private var failedTestsCount: Int = 0

    override fun testPlanExecutionStarted(testPlan: TestPlan) {
        startTimestamp = System.currentTimeMillis()
    }

    override fun executionFinished(testIdentifier: TestIdentifier, testExecutionResult: TestExecutionResult) {
        if (testExecutionResult.status == TestExecutionResult.Status.FAILED) failedTestsCount++
    }

    override fun testPlanExecutionFinished(testPlan: TestPlan) {
        val timeNeeded = (System.currentTimeMillis() - startTimestamp).milliseconds
        checkDebug(testPlan)

        if (failedTestsCount == 0) {
            val allTestClasses = testPlan.allEffectiveContainerJavaClasses
            val notAnnotatedTestClasses = testPlan.allEffectiveContainerJavaClasses.withoutAnnotation<Execution>()
            val nonConcurrentTestClasses = testPlan.allEffectiveContainerJavaClasses.withAnnotation<Execution> { it.value != CONCURRENT }

            if (notAnnotatedTestClasses.isEmpty()) {
                val quantity =
                    if (nonConcurrentTestClasses.isEmpty()) "All" else "${allTestClasses.size - nonConcurrentTestClasses.size}/${allTestClasses.size}"
                listOf(
                    ANSI.termColors.bold("Done. All tests passed within $timeNeeded"),
                    "$quantity test containers running in $CONCURRENT mode."
                )
                    .joinToString("\n")
                    .wrapWithBorder(padding = 2, margin = 1, ansiCode = ANSI.termColors.green)
                    .also { echo(it) }
            } else {
                listOf(
                    ANSI.termColors.bold("Done. All tests passed within $timeNeeded"),
                    "",
                    ANSI.termColors.bold("Non-concurrent test classes: ") + format(nonConcurrentTestClasses),
                    "",
                    ANSI.termColors.bold("Missing @Execution annotation: ") + format(notAnnotatedTestClasses),
                )
                    .joinToString("\n")
                    .draw.border.double(padding = 2, margin = 1, ansiCode = ANSI.termColors.yellow)
                    .also { echo(it) }
            }
        } else {
            listOf(
                ANSI.termColors.bold("Done. BUT $failedTestsCount tests failed!"),
            )
                .joinToString("\n")
                .draw.border.spikedOutward(padding = 2, margin = 1, ansiCode = ANSI.termColors.red)
                .also { echo(it) }
        }
    }

    private fun checkDebug(testPlan: TestPlan) {
        val debugAnnotatedMethods = testPlan.allTestJavaMethods.withAnnotation<Debug> { debug -> debug.includeInReport }
        if (debugAnnotatedMethods.isNotEmpty()) {
            listOf(
                ANSI.termColors.bold("Attention!"),
                "You only see the results of the",
                "${debugAnnotatedMethods.size} @${Debug::class.simpleName} annotated tests.",
                ANSI.termColors.bold("Don't forget to remove them."),
            )
                .joinToString("\n")
                .wrapWithBorder(padding = 2, margin = 1, ansiCode = ANSI.termColors.yellow)
                .also { echo(it) }
        }
    }

    private fun format(nonConcurrentTestClasses: List<AnnotatedElement>): String =
        if (nonConcurrentTestClasses.isEmpty()) "—" else nonConcurrentTestClasses.joinToString("\n",
            prefix = "\n",
            limit = 10) { it.toString() }
}
