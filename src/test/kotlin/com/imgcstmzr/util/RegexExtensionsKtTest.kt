package com.imgcstmzr.util

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.Assertion
import strikt.assertions.isNotNull
import strikt.assertions.isNull

@Execution(CONCURRENT)
class RegexExtensionsKtTest

fun <T : CharSequence> Assertion.Builder<T>.entirelyMatchedBy(regex: Regex) =
    get("entirely matched by $regex") { regex.matchEntire(this) }.isNotNull()

fun Assertion.Builder<Regex>.matchEntire(input: CharSequence): Assertion.Builder<MatchResult> =
    get("match entirely ${input.debug}") { matchEntire(input) }.isNotNull()

fun Assertion.Builder<Regex>.matchEntire(input: CharSequence, expected: Boolean = true): Assertion.Builder<out MatchResult?> =
    get("match entirely ${input.debug}") { matchEntire(input) }.run { if (expected) not { isNull() } else isNull() }

fun Assertion.Builder<MatchResult>.group(groupName: String) =
    get("group with name $groupName: %s") { namedGroups[groupName] }

fun Assertion.Builder<MatchResult>.group(index: Int) =
    get("group with index $index: %s") { groups[index] }

val Assertion.Builder<MatchResult>.groupValues
    get() = get("group values: %s") { groupValues }

val Assertion.Builder<MatchGroup?>.value
    get() = get("value %s") { this?.value }

