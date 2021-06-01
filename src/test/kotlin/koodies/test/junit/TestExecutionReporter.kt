package koodies.test.junit

import koodies.debug.Debug
import koodies.docker.Docker
import koodies.docker.DockerRequiring
import koodies.test.allContainerJavaClasses
import koodies.test.allTestJavaMethods
import koodies.test.withAnnotation
import koodies.text.ANSI.Formatter.Companion.fromScratch
import koodies.text.ANSI.Text.Companion.ansi
import koodies.text.ANSI.ansiRemoved
import koodies.text.LineSeparators.LF
import koodies.text.Semantics.formattedAs
import koodies.text.styling.draw
import koodies.text.styling.wrapWithBorder
import koodies.toSimpleString
import koodies.unit.milli
import koodies.time.seconds
import org.junit.jupiter.api.extension.TestWatcher
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import org.junit.platform.engine.TestExecutionResult
import org.junit.platform.launcher.TestExecutionListener
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.lang.reflect.Method
import kotlin.properties.Delegates

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
        val timeNeeded = (System.currentTimeMillis() - startTimestamp).milli.seconds
        checkDebug(testPlan)
        checkSkippedDockerTests(testPlan)

        if (failedTestsCount == 0) {
            val allTestClasses = testPlan.allContainerJavaClasses
            val concurrentTestClasses = testPlan.allContainerJavaClasses.withAnnotation<Execution> { it.value == CONCURRENT }

            val quantity = "All".takeUnless { concurrentTestClasses.size < allTestClasses.size }
                ?: "${concurrentTestClasses.size}/${allTestClasses.size}"

            listOf(
                "Done. All tests passed within ${timeNeeded.formattedAs.debug}.".ansi.bold,
                "$quantity test containers run concurrently."
            )
                .joinToString(LF)
                .wrapWithBorder(padding = 2, margin = 1, formatter = fromScratch { green })
                .also { println(it) }
        } else {
            listOf(
                "Done. BUT $failedTestsCount tests failed!".ansi.bold,
            )
                .joinToString(LF)
                .draw.border.spikedOutward(padding = 2, margin = 1, fromScratch { red })
                .also { println(it) }
        }
    }

    private fun checkDebug(testPlan: TestPlan) {
        val debugAnnotatedMethods = testPlan.allTestJavaMethods.withAnnotation<Debug>(ancestorsIgnored = false) { debug -> debug.includeInReport }
        if (debugAnnotatedMethods.isNotEmpty()) {
            listOf(
                "${Debug::class.simpleName} in use!".formattedAs.warning,
                "You are only seeing the results of the ${debugAnnotatedMethods.size} annotated tests.",
                "Don't forget to remove them.".ansi.bold,
            )
                .joinToString(LF)
                .wrapWithBorder(padding = 2, margin = 1, formatter = fromScratch { yellow })
                .also { println(it) }
        }
    }

    private fun checkSkippedDockerTests(testPlan: TestPlan) {
        if (Docker.engineRunning) return

        val skipped = testPlan.allTestJavaMethods.withAnnotation<DockerRequiring>(ancestorsIgnored = false)
        if (skipped.isNotEmpty()) {
            val groupBy = skipped.groupBy { testElement: Any ->
                val container = (testElement as? Method)?.declaringClass ?: testElement
                container.toSimpleString().ansiRemoved.split(".")[0]
            }.map { (group, elements) -> "$group: ${elements.size}" }

            listOf(
                "Docker is not running: ${skipped.size} tests skipped!".formattedAs.warning,
                *groupBy.toTypedArray(),
            )
                .wrapWithBorder(padding = 2, margin = 1, formatter = fromScratch { yellow })
                .also { println(it) }
        }
    }
}