package com.bkahlert.koodies.unit

import java.math.BigInteger

interface UnitPrefix {
    val symbol: String
    val prefix: String
    val factor: BigInteger
}
