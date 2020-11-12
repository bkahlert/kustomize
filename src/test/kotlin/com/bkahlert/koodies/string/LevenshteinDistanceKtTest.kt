package com.bkahlert.koodies.string

import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.test.junit.Slow
import com.imgcstmzr.util.quoted
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isGreaterThan
import strikt.assertions.isLessThan
import strikt.assertions.isLessThanOrEqualTo
import kotlin.system.measureTimeMillis
import kotlin.time.milliseconds
import kotlin.time.seconds

@Execution(CONCURRENT)
class LevenshteinDistanceKtTest {

    @Suppress("SpellCheckingInspection")
    @Execution(CONCURRENT)
    @ConcurrentTestFactory
    fun `should calc Levenshtein distance 0`() = listOf(
        "h1" to "h1",
        "gil" to "gil",
    ).map { (from, to) ->
        dynamicTest("${from.quoted} ↔︎ ${to.quoted}") {
            expectThat(from).levenshteinDistance(to).isEqualTo(0)
        }
    }

    @Suppress("SpellCheckingInspection")
    @Execution(CONCURRENT)
    @ConcurrentTestFactory
    fun `should calc Levenshtein distance 1`() = listOf(
        "gil" to "gill",
        "waht" to "what",
        "waht" to "wait",
        "Damerau" to "Damreau",
    ).map { (from, to) ->
        dynamicTest("${from.quoted} ↔︎ ${to.quoted}") {
            expectThat(from).levenshteinDistance(to).isEqualTo(1)
        }
    }

    @Suppress("SpellCheckingInspection")
    @Execution(CONCURRENT)
    @ConcurrentTestFactory
    fun `should calc Levenshtein distance 2`() = listOf(
        "ca" to "abc",
        "thaw" to "what",
        "Damerau" to "uameraD",
        "Damerau" to "Daremau",
        "waht" to "whit",
        "what" to "wtah",
    ).map { (from, to) ->
        dynamicTest("${from.quoted} ↔︎ ${to.quoted}") {
            expectThat(from).levenshteinDistance(to).isEqualTo(2)
        }
    }

    @Nested
    inner class Fuzzy {
        @Test @Slow
        fun `should calculate fuzzy distance between similar strings`() {
            val a = ClassPath("guestfish.boot").readText() + "abc"
            val b = "xyz" + ClassPath("guestfish.boot").readText()

            expectThat(measureTimeMillis { expectThat(a).fuzzyLevenshteinDistance(b).isLessThan(0.05) }.milliseconds)
                .isLessThanOrEqualTo(5.seconds)
        }

        @Test @Slow
        fun `should calculate fuzzy distance between completely different strings`() {
            val a = ClassPath("raspberry.boot").readText()
            val b = ClassPath("guestfish.boot").readText()

            expectThat(measureTimeMillis { expectThat(a).fuzzyLevenshteinDistance(b).isGreaterThan(0.85) }.milliseconds)
                .isLessThanOrEqualTo(5.seconds)
        }
    }
}

fun <T : CharSequence> Assertion.Builder<T>.levenshteinDistance(other: CharSequence): Assertion.Builder<Int> =
    get("Levenshtein distance") { this.levenshteinDistance(other) }


fun <T : CharSequence> Assertion.Builder<T>.fuzzyLevenshteinDistance(other: CharSequence): Assertion.Builder<Double> =
    get("fuzzy Levenshtein distance") {
        val thisString = asString()
        val otherString = other.asString()
        when {
            thisString.length == otherString.length && thisString == otherString ->
                0.0
            thisString.length > 1000 && otherString.length > 1000 ->
                (thisString.take(500).levenshteinDistance(otherString.take(500)) +
                    thisString.takeLast(500).levenshteinDistance(otherString.takeLast(500))) / 1000.0
            else ->
                this.levenshteinDistance(other).toDouble() / maxOf(thisString.length, otherString.length).toDouble()
        }
    }



