package com.bkahlert.koodies.unit

import com.bkahlert.koodies.number.toBigInteger
import java.math.BigInteger
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

@Suppress("SpellCheckingInspection", "EnumEntryName")
enum class BinaryPrefix(
    private val _symbol: String,
    private val exponent: Int,
    private val _factor: BigInteger = if (exponent > 0) BigInteger.valueOf(2).pow(exponent) else BigInteger.ZERO,
) : UnitPrefix, ReadOnlyProperty<Number, BigInteger> {
    Yobi("Yi", 80),
    Zebi("Zi", 70),
    Exbi("Ei", 60),
    Pebi("Pi", 50),
    Tebi("Ti", 40),
    Gibi("Gi", 30),
    Mebi("Mi", 20),
    Kibi("Ki", 10),
    mibi("mi", -10),
    mubi("ui", -20),
    nabi("ni", -30),
    pibi("pi", -40),
    fembi("fi", -50),
    abi("ai", -60),
    zebi("Zi", -70),
    yobi("Yi", -80),
    ;

    override val symbol: String get() = _symbol
    override val prefix: String get() = name.toLowerCase()
    override val factor: BigInteger get() = _factor
    override fun getValue(thisRef: Number, property: KProperty<*>): BigInteger =
        thisRef.also { if (exponent < 0) println("Small $this are currently not fully supported!") }.toBigInteger() * factor
}

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.Yobi]. */
val Number.Yobi: BigInteger by BinaryPrefix.Yobi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.Zebi]. */
val Number.Zebi: BigInteger by BinaryPrefix.Zebi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.Exbi]. */
val Number.Exbi: BigInteger by BinaryPrefix.Exbi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.Pebi]. */
val Number.Pebi: BigInteger by BinaryPrefix.Pebi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.Tebi]. */
val Number.Tebi: BigInteger by BinaryPrefix.Tebi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.Gibi]. */
val Number.Gibi: BigInteger by BinaryPrefix.Gibi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.Mebi]. */
val Number.Mebi: BigInteger by BinaryPrefix.Mebi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.Kibi]. */
val Number.Kibi: BigInteger by BinaryPrefix.Kibi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.mibi]. */
val Number.mibi: BigInteger by BinaryPrefix.mibi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.mubi]. */
val Number.mubi: BigInteger by BinaryPrefix.mubi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.nabi]. */
val Number.nabi: BigInteger by BinaryPrefix.nabi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.pibi]. */
val Number.pibi: BigInteger by BinaryPrefix.pibi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.fembi]. */
val Number.fembi: BigInteger by BinaryPrefix.fembi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.abi]. */
val Number.abi: BigInteger by BinaryPrefix.abi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.zebi]. */
val Number.zebi_: BigInteger by BinaryPrefix.zebi

/** Returns a [BigInteger] equal to this [Number] times [BinaryPrefix.yobi]. */
val Number.yobi_: BigInteger by BinaryPrefix.yobi
