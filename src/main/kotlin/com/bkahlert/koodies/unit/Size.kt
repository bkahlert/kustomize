package com.bkahlert.koodies.unit

import com.bkahlert.koodies.number.BigDecimalConstants
import com.bkahlert.koodies.number.formatToExactDecimals
import com.bkahlert.koodies.number.scientificFormat
import com.bkahlert.koodies.number.toBigDecimal
import com.imgcstmzr.util.exists
import com.imgcstmzr.util.isDirectory
import com.imgcstmzr.util.isSymlink
import java.io.File
import java.math.BigDecimal
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass


inline class Size(val bytes: BigDecimal) : Comparable<Size> {

    companion object {
        val ZERO: Size = Size(BigDecimal.ZERO)
        val supportedPrefixes: Map<KClass<out UnitPrefix>, List<UnitPrefix>> = mapOf(
            BinaryPrefix::class to listOf(
                BinaryPrefix.Yobi,
                BinaryPrefix.Zebi,
                BinaryPrefix.Exbi,
                BinaryPrefix.Pebi,
                BinaryPrefix.Tebi,
                BinaryPrefix.Gibi,
                BinaryPrefix.Mebi,
                BinaryPrefix.Kibi,
            ),
            DecimalPrefix::class to listOf(
                DecimalPrefix.Yotta,
                DecimalPrefix.Zetta,
                DecimalPrefix.Exa,
                DecimalPrefix.Peta,
                DecimalPrefix.Tera,
                DecimalPrefix.Giga,
                DecimalPrefix.Mega,
                DecimalPrefix.kilo,
            )
        )
        const val SYMBOL = "B"
        fun parse(value: CharSequence) {
            require(value.endsWith(SYMBOL)) { "Size must be in unit bytes (e.g. 5 ${DecimalPrefix.Mega}($SYMBOL})." }
            require(value.endsWith(SYMBOL)) { "Size must be in unit bytes (e.g. 5 ${DecimalPrefix.Mega}($SYMBOL})." }
            // TODO
        }
    }

    /**
     * Returns a string representation of this size value expressed in the unit
     * with the [UnitPrefix] which yields the most compact and readable number value.
     *
     * Special cases:
     *  - zero size is formatted as `"0 B"`
     *  - fraction sizes are formatted as `"0 B"`
     *  - very big sizes (more than a million [Yotta]byte/[Yobi]byte) are expressed
     *  in [Yotta]byte/[Yobi]byte and formatted in scientific notation
     *
     * @return the value of size in the automatically determined [UnitPrefix], e.g. 42.2 MB.
     */
    override fun toString(): String = toString(DecimalPrefix::class)

    /**
     * Returns a string representation of this size value expressed in the unit
     * with the [UnitPrefix] which yields the most compact and readable number value.
     *
     * Special cases:
     *  - zero size is formatted as `"0 B"`
     *  - fraction sizes are formatted as `"0 B"`
     *  - very big sizes (more than a million [Yotta]byte/[Yobi]byte) are expressed
     *  in [Yotta]byte/[Yobi]byte and formatted in scientific notation
     *
     * @return the value of size in the automatically determined [UnitPrefix], e.g. 42.2 MB.
     */
    fun toString(prefixType: KClass<out UnitPrefix>): String {
        val prefixes: List<UnitPrefix>? = supportedPrefixes[prefixType]
        require(prefixes != null) { "$prefixType is not supported. Valid options are: " + supportedPrefixes.keys }
        return when (bytes) {
            BigDecimal.ZERO -> "0 $SYMBOL"
            else -> {
                val absNs = bytes.abs()
                var scientific = false
                val index = prefixes.dropLastWhile { absNs >= it.factor }.size
                val millionish = prefixes.first().basis.toBigDecimal().pow(2 * prefixes.first().baseExponent)
                if (index == 0 && absNs >= prefixes.first().factor * millionish) scientific = true
                val prefix = prefixes.getOrNull(index)
                val value = bytes / prefix.factor
                val formattedValue = when {
                    scientific -> value.scientificFormat
                    else -> {
                        val decimals = precision(value.abs(), prefix)
                        value.formatToExactDecimals(decimals)
                    }
                }
                "$formattedValue ${prefix.getSymbol<Size>()}$SYMBOL"
            }
        }
    }

    /**
     * Returns a string representation of this size value expressed with the given [unitPrefix]
     * and formatted with the specified [decimals] number of digits after decimal point.
     *
     * @return the value of duration in the specified [unitPrefix]
     *
     * @throws IllegalArgumentException if [decimals] is less than zero.
     */
    fun toString(unitPrefix: UnitPrefix, decimals: Int = 0): String {
        require(decimals >= 0) { "decimals must be not negative, but was $decimals" }
        val number = bytes / unitPrefix.factor
        val upperDetailLimit = 1e14.toBigDecimal()
        return when {
            number.abs() < upperDetailLimit -> number.formatToExactDecimals(decimals.coerceAtMost(12))
            else -> number.scientificFormat
        } + " " + unitPrefix.getSymbol<Size>() + SYMBOL
    }

    /*
      if (unit == null) return 0
        for (i in 0 until unit.baseExponent) {
            val pow = unit.basis.pow(i)
            if (value < pow) return unit.baseExponent - i
        }
        return 0
     */
    private fun precision(value: BigDecimal, unit: UnitPrefix?): Int = when (unit) {
        null -> 0
        else -> when {
            value < BigDecimal.ONE -> 3
            value < BigDecimal.TEN -> 2
            value < BigDecimalConstants.HUNDRED -> 1
            else -> 0
        }
    }

    override fun compareTo(other: Size): Int = this.bytes.compareTo(other.bytes)
    operator fun plus(other: Size): Size = Size(bytes + other.bytes)
    operator fun plus(otherBytes: Long): Size = Size(bytes + BigDecimal.valueOf(otherBytes))
    operator fun plus(otherBytes: Int): Size = Size(bytes + BigDecimal.valueOf(otherBytes.toLong()))
    operator fun minus(other: Size): Size = Size(bytes - other.bytes)
    operator fun minus(otherBytes: Long): Size = Size(bytes - BigDecimal.valueOf(otherBytes))
    operator fun minus(otherBytes: Int): Size = Size(bytes - BigDecimal.valueOf(otherBytes.toLong()))
    operator fun unaryMinus(): Size = ZERO - this
    fun toZeroFilledByteArray(): ByteArray = ByteArray(bytes.toInt())
}

val Number.bytes: Size get() = if (this == 0) Size.ZERO else Size(toBigDecimal())

val Path.size: Size
    get() {
        require(exists) { "$this does not exist" }
        return if (!isDirectory) Files.size(this).bytes
        else (toFile().listFiles() ?: return Size.ZERO)
            .asSequence()
            .map(File::toPath)
            .filterNot(Path::isSymlink)
            .fold(Size.ZERO) { size, path -> size + path.size }
    }

fun Size.`in`(unit: UnitPrefix?) = bytes / (unit?.factor?.toBigDecimal() ?: BigDecimal.ONE)