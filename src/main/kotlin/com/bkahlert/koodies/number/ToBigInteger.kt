package com.bkahlert.koodies.number

import java.math.BigDecimal
import java.math.BigInteger

/**
 * Returns the value of this number as a [BigInteger], which may involve rounding.
 */
fun Number.toBigInteger(): BigInteger = when (this) {
    is BigInteger -> this
    is BigDecimal -> this.toBigInteger()
    else -> BigInteger.valueOf(toLong())
}
