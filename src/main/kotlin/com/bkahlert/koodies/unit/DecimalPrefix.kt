@file:Suppress("EnumEntryName")

package com.bkahlert.koodies.unit

import com.bkahlert.koodies.number.toBigInteger
import java.math.BigInteger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Suppress("KDocMissingDocumentation")
enum class DecimalPrefix(
    private val _symbol: String,
    private val exponent: Int,
    private val _factor: BigInteger = if (exponent > 0) BigInteger.valueOf(10).pow(exponent) else BigInteger.ZERO,
) : UnitPrefix, ReadOnlyProperty<Number, BigInteger> {
    Yotta("Y", 24),
    Zetta("Z", 21),
    Exa("E", 18),
    Peta("P", 15),
    Tera("T", 12),
    Giga("G", 9),
    Mega("M", 6),
    kilo("k", 3),
    hecto("h", 2),
    deca("da", 1),
    deci("d", -1),
    centi("c", -2),
    milli("m", -3),
    micro("μ", -6),
    nano("n", -9),
    pico("p", -12),
    femto("f", -15),
    atto("a", -18),
    zepto("z", -21),
    yocto("y", -24),
    ;

    override val symbol: String get() = _symbol
    override val prefix: String get() = name.toLowerCase()
    override val factor: BigInteger get() = _factor
    override fun getValue(thisRef: Number, property: KProperty<*>): BigInteger =
        thisRef.also { if (exponent < 0) println("Small $this are currently not fully supported!") }.toBigInteger() * factor
}


/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.Yotta]. */
val Number.Yotta: BigInteger by DecimalPrefix.Yotta

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.Zetta]. */
val Number.Zetta: BigInteger by DecimalPrefix.Zetta

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.Exa]. */
val Number.Exa: BigInteger by DecimalPrefix.Exa

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.Peta]. */
val Number.Peta: BigInteger by DecimalPrefix.Peta

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.Tera]. */
val Number.Tera: BigInteger by DecimalPrefix.Tera

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.Giga]. */
val Number.Giga: BigInteger by DecimalPrefix.Giga

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.Mega]. */
val Number.Mega: BigInteger by DecimalPrefix.Mega

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.kilo]. */
val Number.kilo: BigInteger by DecimalPrefix.kilo

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.hecto]. */
val Number.hecto: BigInteger by DecimalPrefix.hecto

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.deca]. */
val Number.deca: BigInteger by DecimalPrefix.deca

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.deci]. */
val Number.deci: BigInteger by DecimalPrefix.deci

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.centi]. */
val Number.centi: BigInteger by DecimalPrefix.centi

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.milli]. */
val Number.milli: BigInteger by DecimalPrefix.milli

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.micro]. */
val Number.micro: BigInteger by DecimalPrefix.micro

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.nano]. */
val Number.nano: BigInteger by DecimalPrefix.nano

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.pico]. */
val Number.pico: BigInteger by DecimalPrefix.pico

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.femto]. */
val Number.femto: BigInteger by DecimalPrefix.femto

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.atto]. */
val Number.atto: BigInteger by DecimalPrefix.atto

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.zepto]. */
val Number.zepto: BigInteger by DecimalPrefix.zepto

/** Returns a [BigInteger] equal to this [Number] times [DecimalPrefix.yocto]. */
val Number.yocto: BigInteger by DecimalPrefix.yocto
