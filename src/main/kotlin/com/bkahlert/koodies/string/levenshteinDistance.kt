package com.bkahlert.koodies.string

fun CharSequence.levenshteinDistance(other: CharSequence): Int {
    val cost = Array(length + 1) { IntArray(other.length + 1) }
    (0..length).forEach { i -> cost[i][0] = i }
    (0..other.length).forEach { i -> cost[0][i] = i }

    val thisInOtherIndex = hashMapOf<Char, Int>()
    (1..length).forEach { thisIndex ->
        var prevMatchingOtherIndex = 0
        (1..other.length).forEach { otherIndex ->
            val doesPreviousMatch = (this[thisIndex - 1] == other[otherIndex - 1])

            val possibleCosts = mutableListOf<Int>()
            if (doesPreviousMatch) possibleCosts.add(cost[thisIndex - 1][otherIndex - 1]) // perfect match cost
            else possibleCosts.add(cost[thisIndex - 1][otherIndex - 1] + 1) // substitution cost
            possibleCosts.add(cost[thisIndex][otherIndex - 1] + 1) // insertion cost
            possibleCosts.add(cost[thisIndex - 1][otherIndex] + 1) // deletion cost

            val otherToThisIndex = thisInOtherIndex.getOrDefault(other[otherIndex - 1], 0) // transposition cost
            if (otherToThisIndex != 0 && prevMatchingOtherIndex != 0)
                possibleCosts += cost[otherToThisIndex - 1][prevMatchingOtherIndex - 1] +
                    (thisIndex - otherToThisIndex - 1) + 1 + (otherIndex - prevMatchingOtherIndex - 1)

            cost[thisIndex][otherIndex] = possibleCosts.minOrNull() ?: error("must not happen")

            if (doesPreviousMatch) prevMatchingOtherIndex = otherIndex
        }
        thisInOtherIndex[this[thisIndex - 1]] = thisIndex
    }
    return cost[length][other.length]
}