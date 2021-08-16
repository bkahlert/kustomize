package com.bkahlert.kommons.io.file

import com.bkahlert.kommons.io.path.pathString
import strikt.api.Assertion
import java.nio.file.Path

fun <T : Path> Assertion.Builder<T>.isSiblingOf(expected: Path, order: Int = 1) =
    assert("is sibling of order $order") { actual ->
        val actualNames = actual.map { name -> name.pathString }.toList()
        val otherNames = expected.map { name -> name.pathString }.toList()
        val actualIndex = actualNames.size - order - 1
        val otherIndex = otherNames.size - order - 1
        val missing = (actualIndex - otherNames.size + 1)
        if (missing > 0) {
            fail("$expected is too short. At least $missing segments are missing to be able to be sibling.")
        }
        if (missing <= 0) {
            val evaluation = actualNames.zip(otherNames).mapIndexed { index, namePair ->
                val match = if (index == actualIndex || index == otherIndex) true
                else namePair.first == namePair.second
                namePair to match
            }
            val matches = evaluation.takeWhile { (_, match) -> match }.map { (namePair, _) -> namePair.first }
            val misMatch = evaluation.getOrNull(matches.size)?.let { (namePair, _) -> namePair }
            if (misMatch != null) fail("Paths match up to $matches, then mismatch $misMatch")
            else pass()
        }
    }
