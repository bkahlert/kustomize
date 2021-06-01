package koodies.test.junit

import koodies.collections.maxOrThrow
import koodies.collections.minOrThrow
import koodies.unit.milli
import koodies.time.seconds
import strikt.api.Assertion

fun <T : Enum<T>> Assertion.Builder<List<T>>.ranConcurrently(startTimes: Map<T, Long>, endTimes: Map<T, Long>): Assertion.Builder<List<T>> =
    assert("ran concurrently") { tests ->
        val relevantStartTimes = startTimes.filterKeys { it in tests }
        val relevantEndTimes = endTimes.filterKeys { it in tests }

        val startDifference = relevantStartTimes.values.run { maxOrThrow() - minOrThrow() }.milli.seconds
        val endDifference = relevantEndTimes.values.run { maxOrThrow() - minOrThrow() }.milli.seconds
        val overallDifference = (relevantEndTimes.values.maxOrThrow() - relevantStartTimes.values.minOrThrow()).milli.seconds
        when {
            startDifference < .5.seconds -> fail("$startDifference difference between start times")
            endDifference < .5.seconds -> fail("$endDifference difference between end times")
            overallDifference < .5.seconds -> fail("$overallDifference difference between first start and last end time")
            else -> pass()
        }
    }
