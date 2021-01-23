package com.imgcstmzr

import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.JUnit.render
import com.imgcstmzr.test.JUnit.runTests
import com.imgcstmzr.test.Slow
import com.imgcstmzr.test.UniqueId
import koodies.debug.asEmoji
import koodies.logging.SLF4J
import koodies.runtime.deleteOnExit
import koodies.terminal.AnsiCode.Companion.removeEscapeSequences
import koodies.terminal.AnsiColors.green
import koodies.terminal.AnsiColors.red
import koodies.text.LineSeparators.LF
import koodies.text.styling.Borders
import koodies.text.styling.Boxes
import koodies.text.styling.Boxes.Companion.wrapWithBox
import koodies.text.styling.draw
import koodies.text.styling.wrapWithBorder
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest
import org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage
import org.junit.platform.launcher.TagFilter.excludeTags
import org.junit.platform.launcher.TagFilter.includeTags
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder
import org.junit.platform.launcher.listeners.LoggingListener
import org.junit.platform.launcher.listeners.SummaryGeneratingListener
import org.junit.platform.launcher.listeners.TestExecutionSummary
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.exists
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty
import kotlin.system.exitProcess
import koodies.debug.debug as koodiesDebug

private val root by lazy { createTempDirectory("koodies").deleteOnExit() }
val Any?.debug get() = koodiesDebug.removeEscapeSequences()

/**
 * Calculates the display name for a test with the specified [subject]
 * and the optional [testNamePattern] which supports curly placeholders `{}` like [SLF4J] does.
 *
 * If no [testNamePattern] is specified a [displayNameFallback] is calculated heuristically.
 *
 * @see displayNameFallback
 */
inline fun <reified T> displayName(subject: T, testNamePattern: String? = null): String {
    val (fallbackPattern: String, args: Array<*>) = displayNameFallback(subject)
    return SLF4J.format(testNamePattern ?: fallbackPattern, *args)
}

/**
 * Attempts to calculates a rich display name for a test case testing the specified [subject].
 */
inline fun <reified T> displayNameFallback(subject: T) = when (subject) {
    is KFunction<*> -> "for property: {}" to arrayOf(subject.name)
    is KProperty<*> -> "for property: {}" to arrayOf(subject.name)
    is Triple<*, *, *> -> "for: {} to {} to {}" to arrayOf(subject.first.debug, subject.second.debug, subject.third.debug)
    is Pair<*, *> -> "for: {} to {}" to arrayOf(subject.first.debug, subject.second.debug)
    else -> "for: {}" to arrayOf(subject.debug)
}

/**
 * Creates one [DynamicTest] for each [T].
 *
 * The name for each test is heuristically derived but can also be explicitly specified using [testNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
inline fun <reified T> Iterable<T>.test(testNamePattern: String? = null, crossinline executable: (T) -> Unit) = map { subject ->
    DynamicTest.dynamicTest(displayName(subject, testNamePattern)) { executable(subject) }
}

/**
 * Creates one [DynamicTest] for each [T].
 *
 * The name for each test is heuristically derived but can also be explicitly specified using [testNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
inline fun <reified T> Array<T>.test(testNamePattern: String? = null, crossinline executable: (T) -> Unit) = toList().test(testNamePattern, executable)

/**
 * Creates one [DynamicContainer] for each [T] whereas
 * each container will be built using the specified [DynamicTestsBuilder] based [init]
 * (and therefore can be arbitrarily complex).
 *
 * The name for each container is heuristically derived but can also be explicitly specified using [containerNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
inline fun <reified T> Iterable<T>.tests(containerNamePattern: String? = null, noinline init: DynamicTestsBuilder<T>.(T) -> Unit): List<DynamicContainer> =
    map { subject ->
        DynamicContainer.dynamicContainer(displayName(containerNamePattern), DynamicTestsBuilder.build(subject, init))
    }

/**
 * Builder for arbitrary test trees consisting of instances of [DynamicContainer] and [DynamicTest].
 */
interface DynamicTestsBuilder<T> {
    /**
     * Builds a [DynamicContainer] using the specified [name] and the specified [executable].
     */
    fun container(name: String, init: DynamicTestsBuilder<T>.(T) -> Unit)

    /**
     * Builds a [DynamicTest] using the specified [name] and the specified [executable].
     */
    fun test(name: String, executable: () -> Unit)

    companion object {
        /**
         * Builds an arbitrary test trees to test all necessary aspect of the specified [subject].
         */
        fun <T> build(subject: T, init: DynamicTestsBuilder<T>.(T) -> Unit): List<DynamicNode> =
            mutableListOf<DynamicNode>().apply {
                object : DynamicTestsBuilder<T> {
                    override fun container(name: String, init: DynamicTestsBuilder<T>.(T) -> Unit) {
                        add(DynamicContainer.dynamicContainer(name, build(subject, init)))
                    }

                    override fun test(name: String, executable: () -> Unit) {
                        add(DynamicTest.dynamicTest(name, executable))
                    }
                }.init(subject)
            }
    }
}

/**
 * Runs the [block] with a temporary directory as its receiver object,
 * leveraging the need to clean up eventually created files.
 *
 * The name is generated from the test name and a random suffix.
 *
 * @throws IllegalStateException if called from outside of a test
 */
fun withTempDir(uniqueId: UniqueId, block: Path.() -> Unit) {
    val tempDir = root.resolve(uniqueId.simple).createDirectories()
    tempDir.block()
    check(root.exists()) {
        println("The shared root temp directory was deleted by $uniqueId or a concurrently running test. This must not happen.".red())
        exitProcess(-1)
    }
}

/**
 * Creates a [DynamicTest] for each [T]—providing each test with a temporary work directory
 * that is automatically deletes after execution as the receiver object.
 *
 * The name for each test is heuristically derived but can also be explicitly specified using [testNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
inline fun <reified T> Iterable<T>.testWithTempDir(uniqueId: UniqueId, testNamePattern: String? = null, crossinline executable: Path.(T) -> Unit) =
    test(testNamePattern) { withTempDir(uniqueId) { executable(it) } }

/**
 * Creates a [DynamicTest] for each map entry—providing each test with a temporary work directory
 * that is automatically deletes after execution as the receiver object.
 *
 * The name for each test is heuristically derived but can also be explicitly specified using [testNamePattern]
 * which supports curly placeholders `{}` like [SLF4J] does.
 */
inline fun <reified K, reified V> Map<K, V>.testWithTempDir(
    uniqueId: UniqueId,
    testNamePattern: String? = null,
    crossinline executable: Path.(Pair<K, V>) -> Unit,
) = toList().test(testNamePattern) { withTempDir(uniqueId) { executable(it) } }


object Tests {

    private fun runTaggedTests(
        name: String,
        launcherDiscoveryRequestBuilder: LauncherDiscoveryRequestBuilder.() -> Unit,
    ): SummaryGeneratingListener {
        val verticalSpace = LF.repeat(5)
        println(verticalSpace + name.wrapWithBox(Boxes.SPHERICAL).wrapWithBorder(Borders.Rounded, padding = 10, margin = 15).green() + verticalSpace)
        return runTests(
            selectPackage("koodies"),
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
