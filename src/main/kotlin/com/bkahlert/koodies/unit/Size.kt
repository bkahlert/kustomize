package com.bkahlert.koodies.unit

import com.bkahlert.koodies.number.toBigInteger
import com.imgcstmzr.util.exists
import com.imgcstmzr.util.isDirectory
import com.imgcstmzr.util.isSymlink
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path

inline class Size(val bytes: BigInteger) : Comparable<Size> {

    private fun format(vararg prefixes: UnitPrefix): String =
        prefixes
            .map { prefix -> bytes / prefix.factor to prefix }
            .firstOrNull { (ratio, _) -> ratio > BigInteger.ZERO }
            ?.let { (ratio, unit) -> "$ratio ${unit.symbol}B" }
            ?: "$bytes $symbol"

    val formatted: String
        get() = format(
            DecimalPrefix.Yotta,
            DecimalPrefix.Zetta,
            DecimalPrefix.Exa,
            DecimalPrefix.Peta,
            DecimalPrefix.Tera,
            DecimalPrefix.Giga,
            DecimalPrefix.Mega,
            DecimalPrefix.kilo,
        )

    val binaryFormatted: String
        get() = format(
            BinaryPrefix.Yobi,
            BinaryPrefix.Zebi,
            BinaryPrefix.Exbi,
            BinaryPrefix.Pebi,
            BinaryPrefix.Tebi,
            BinaryPrefix.Gibi,
            BinaryPrefix.Mebi,
            BinaryPrefix.Kibi,
        )

    override fun toString(): String = formatted

    companion object {
        val ZERO: Size = Size(BigInteger.ZERO)
        private const val symbol = "B"
    }

    override fun compareTo(other: Size): Int = this.bytes.compareTo(other.bytes)
    operator fun plus(other: Size): Size = Size(bytes + other.bytes)
    operator fun plus(otherBytes: Long): Size = Size(bytes + BigInteger.valueOf(otherBytes))
    operator fun plus(otherBytes: Int): Size = Size(bytes + BigInteger.valueOf(otherBytes.toLong()))
    operator fun minus(other: Size): Size = Size(bytes - other.bytes)
    operator fun minus(otherBytes: Long): Size = Size(bytes - BigInteger.valueOf(otherBytes))
    operator fun minus(otherBytes: Int): Size = Size(bytes - BigInteger.valueOf(otherBytes.toLong()))
    fun toZeroFilledByteArray(): ByteArray = ByteArray(bytes.toInt())
}

val Number.bytes: Size get() = if (this == 0) Size.ZERO else Size(toBigInteger())

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
