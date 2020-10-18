package com.bkahlert.koodies.number

import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.unit.BinaryPrefix
import com.bkahlert.koodies.unit.DecimalPrefix
import com.bkahlert.koodies.unit.Exa
import com.bkahlert.koodies.unit.Exbi
import com.bkahlert.koodies.unit.Gibi
import com.bkahlert.koodies.unit.Giga
import com.bkahlert.koodies.unit.Kibi
import com.bkahlert.koodies.unit.Mebi
import com.bkahlert.koodies.unit.Mega
import com.bkahlert.koodies.unit.Pebi
import com.bkahlert.koodies.unit.Peta
import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.Size.Companion.times
import com.bkahlert.koodies.unit.Tebi
import com.bkahlert.koodies.unit.Tera
import com.bkahlert.koodies.unit.Yobi
import com.bkahlert.koodies.unit.Yotta
import com.bkahlert.koodies.unit.Zebi
import com.bkahlert.koodies.unit.Zetta
import com.bkahlert.koodies.unit.abi
import com.bkahlert.koodies.unit.atto
import com.bkahlert.koodies.unit.bytes
import com.bkahlert.koodies.unit.centi
import com.bkahlert.koodies.unit.deca
import com.bkahlert.koodies.unit.deci
import com.bkahlert.koodies.unit.fembi
import com.bkahlert.koodies.unit.femto
import com.bkahlert.koodies.unit.hecto
import com.bkahlert.koodies.unit.kilo
import com.bkahlert.koodies.unit.mibi
import com.bkahlert.koodies.unit.micro
import com.bkahlert.koodies.unit.milli
import com.bkahlert.koodies.unit.mubi
import com.bkahlert.koodies.unit.nabi
import com.bkahlert.koodies.unit.nano
import com.bkahlert.koodies.unit.pibi
import com.bkahlert.koodies.unit.pico
import com.bkahlert.koodies.unit.size
import com.bkahlert.koodies.unit.yobi_
import com.bkahlert.koodies.unit.yocto
import com.bkahlert.koodies.unit.zebi_
import com.bkahlert.koodies.unit.zepto
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.appendText
import com.imgcstmzr.util.times
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.message
import java.nio.file.Path

@Execution(ExecutionMode.CONCURRENT)
internal class SizeTest {

    @Test
    internal fun `should use decimal unit by default`() {
        expectThat(42.Mega.bytes.toString()).isEqualTo("42.0 MB")
    }

    @Nested
    inner class WithBinaryPrefix {

        @ConcurrentTestFactory
        internal fun `should format integer binary form`() = listOf(
            4_200_000.Yobi.bytes to "4.20e+6 YiB",
            42.Yobi.bytes to "42.0 YiB",
            42.Zebi.bytes to "42.0 ZiB",
            42.Exbi.bytes to "42.0 EiB",
            42.Pebi.bytes to "42.0 PiB",
            42.Tebi.bytes to "42.0 TiB",
            42.Gibi.bytes to "42.0 GiB",
            42.Mebi.bytes to "42.0 MiB",
            42.Kibi.bytes to "42.0 KiB",
            42.bytes to "42 B",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString(BinaryPrefix::class)
            val actualNegative = (-size).toString(BinaryPrefix::class)
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            } to dynamicTest("-$expected == $actualNegative ← ${(-size).bytes}") {
                expectThat(actualNegative).isEqualTo("-$expected")
            }
        }.unzip()
            .let { (pos, neg) ->
                listOf(
                    dynamicContainer("positive", pos),
                    dynamicContainer("negative", neg),
                )
            }


        @ConcurrentTestFactory
        internal fun `should format fraction binary form`() = listOf(
            4_200.Gibi.bytes to "4.10 TiB",
            420.Gibi.bytes to "420 GiB",
            42.Gibi.bytes to "42.0 GiB",
            4.2.Gibi.bytes to "4.20 GiB",
            .42.Gibi.bytes to "430 MiB",
            .042.Gibi.bytes to "43.0 MiB",
            .0042.Gibi.bytes to "4.30 MiB",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString(BinaryPrefix::class)
            val actualNegative = (-size).toString(BinaryPrefix::class)
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            } to dynamicTest("-$expected == $actualNegative ← ${(-size).bytes}") {
                expectThat(actualNegative).isEqualTo("-$expected")
            }
        }.unzip()
            .let { (pos, neg) ->
                listOf(
                    dynamicContainer("positive", pos),
                    dynamicContainer("negative", neg),
                )
            }

        @ConcurrentTestFactory
        internal fun `should format 0 binary form`() = listOf(
            0.Yobi.bytes to "0 B",
            0.Kibi.bytes to "0 B",
            0.bytes to "0 B",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString(BinaryPrefix::class)
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            }
        }

        @ConcurrentTestFactory
        internal fun `should throw on yet unsupported prefixes`() = listOf(
            BinaryPrefix.mibi to { number: Int -> number.mibi },
            BinaryPrefix.mubi to { number: Int -> number.mubi },
            BinaryPrefix.nabi to { number: Int -> number.nabi },
            BinaryPrefix.pibi to { number: Int -> number.pibi },
            BinaryPrefix.fembi to { number: Int -> number.fembi },
            BinaryPrefix.abi to { number: Int -> number.abi },
            BinaryPrefix.zebi to { number: Int -> number.zebi_ },
            BinaryPrefix.yobi to { number: Int -> number.yobi_ },
        ).map { (prefix, factory) ->
            dynamicTest("$prefix") {
                expectCatching { factory(0) }
                    .isFailure()
                    .isA<IllegalArgumentException>().message.isEqualTo("Small $prefix are currently not fully supported!")
            }
        }

        @ConcurrentTestFactory
        internal fun `should format to specific unit`() = listOf(
            4_200_000.Yobi.bytes to "4.84e+24 MiB",
            42.Yobi.bytes to "4.84e+19 MiB",
            42.Zebi.bytes to "4.73e+16 MiB",
            42.Exbi.bytes to "46179488366592.0000 MiB",
            42.Pebi.bytes to "45097156608.0000 MiB",
            42.Tebi.bytes to "44040192.0000 MiB",
            42.Gibi.bytes to "43008.0000 MiB",
            42.Mebi.bytes to "42.0000 MiB",
            52.Kibi.bytes to "0.0508 MiB",
            42.Kibi.bytes to "0.0410 MiB",
            42.bytes to "0.0000 MiB",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString(BinaryPrefix.Mebi, 4)
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            }
        }
    }


    @Nested
    inner class WithDecimalPrefix {

        @ConcurrentTestFactory
        internal fun `should format integer decimal form`() = listOf(
            4_200_000.Yotta.bytes to "4.20e+6 YB",
            42.Yotta.bytes to "42.0 YB",
            42.Zetta.bytes to "42.0 ZB",
            42.Exa.bytes to "42.0 EB",
            42.Peta.bytes to "42.0 PB",
            42.Tera.bytes to "42.0 TB",
            42.Giga.bytes to "42.0 GB",
            42.Mega.bytes to "42.0 MB",
            42.kilo.bytes to "42.0 KB",
            42.bytes to "42 B",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString(DecimalPrefix::class)
            val actualNegative = (-size).toString(DecimalPrefix::class)
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            } to dynamicTest("-$expected == $actualNegative ← ${(-size).bytes}") {
                expectThat(actualNegative).isEqualTo("-$expected")
            }
        }.unzip()
            .let { (pos, neg) ->
                listOf(
                    dynamicContainer("positive", pos),
                    dynamicContainer("negative", neg),
                )
            }


        @ConcurrentTestFactory
        internal fun `should format fraction decimal form`() = listOf(
            4_200.Giga.bytes to "4.20 TB",
            420.Giga.bytes to "420 GB",
            42.Giga.bytes to "42.0 GB",
            4.2.Giga.bytes to "4.20 GB",
            .42.Giga.bytes to "420 MB",
            .042.Giga.bytes to "42.0 MB",
            .0042.Giga.bytes to "4.20 MB",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString(DecimalPrefix::class)
            val actualNegative = (-size).toString(DecimalPrefix::class)
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            } to dynamicTest("-$expected == $actualNegative ← ${(-size).bytes}") {
                expectThat(actualNegative).isEqualTo("-$expected")
            }
        }.unzip()
            .let { (pos, neg) ->
                listOf(
                    dynamicContainer("positive", pos),
                    dynamicContainer("negative", neg),
                )
            }

        @ConcurrentTestFactory
        internal fun `should format 0 decimal form`() = listOf(
            0.Yotta.bytes to "0 B",
            0.kilo.bytes to "0 B",
            0.bytes to "0 B",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString()
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            }
        }

        @ConcurrentTestFactory
        internal fun `should throw on yet unsupported prefixes`() = listOf(
            DecimalPrefix.deci to { number: Int -> number.deci },
            DecimalPrefix.centi to { number: Int -> number.centi },
            DecimalPrefix.milli to { number: Int -> number.milli },
            DecimalPrefix.micro to { number: Int -> number.micro },
            DecimalPrefix.nano to { number: Int -> number.nano },
            DecimalPrefix.pico to { number: Int -> number.pico },
            DecimalPrefix.femto to { number: Int -> number.femto },
            DecimalPrefix.atto to { number: Int -> number.atto },
            DecimalPrefix.zepto to { number: Int -> number.zepto },
            DecimalPrefix.yocto to { number: Int -> number.yocto },
        ).map { (prefix, factory) ->
            dynamicTest("$prefix") {
                expectCatching { factory(0) }
                    .isFailure()
                    .isA<IllegalArgumentException>().message.isEqualTo("Small $prefix are currently not fully supported!")
            }
        }

        @ConcurrentTestFactory
        internal fun `should format to specific unit`() = listOf(
            4_200_000.Yotta.bytes to "4.20e+24 MB",
            42.Yotta.bytes to "4.20e+19 MB",
            42.Zetta.bytes to "4.20e+16 MB",
            42.Exa.bytes to "42000000000000.0000 MB",
            42.Peta.bytes to "42000000000.0000 MB",
            42.Tera.bytes to "42000000.0000 MB",
            42.Giga.bytes to "42000.0000 MB",
            42.Mega.bytes to "42.0000 MB",
            520.hecto.bytes to "0.0520 MB", // ⛳️
            420.hecto.bytes to "0.0420 MB", // 🌽
            52.kilo.bytes to "0.0520 MB",
            42.kilo.bytes to "0.0420 MB",
            42.bytes to "0.0000 MB",
        ).map { (size: Size, expected: String) ->
            val actual = size.toString(DecimalPrefix.Mega, 4)
            dynamicTest("$expected == $actual ← ${size.bytes}") {
                expectThat(actual).isEqualTo(expected)
            }
        }
    }


    @Nested
    inner class AsSize {

        val tempFile: Path = Paths.tempFile().apply { 2500.times { appendText("1234567890") } }

        @Test
        internal fun `should format size human-readable (10^x)`() {
            expectThat(tempFile.size.toString(DecimalPrefix::class)).isEqualTo("25.0 KB")
        }

        @Test
        internal fun `should format size human-readable (2^y)`() {
            expectThat(tempFile.size.toString(BinaryPrefix::class)).isEqualTo("24.4 KiB")
        }
    }

    @Nested
    inner class Conversion {
        val binFactor = BinaryPrefix.Kibi.factor
        val decFactor = DecimalPrefix.kilo.factor

        @ConcurrentTestFactory
        internal fun `should format to specific unit`() = listOf(
            42.Yobi.bytes to binFactor * binFactor * binFactor * binFactor * binFactor * binFactor * binFactor * binFactor * 42.bytes,
            42.Zebi.bytes to binFactor * binFactor * binFactor * binFactor * binFactor * binFactor * binFactor * 42.bytes,
            42.Exbi.bytes to binFactor * binFactor * binFactor * binFactor * binFactor * binFactor * 42.bytes,
            42.Pebi.bytes to binFactor * binFactor * binFactor * binFactor * binFactor * 42.bytes,
            42.Tebi.bytes to binFactor * binFactor * binFactor * binFactor * 42.bytes,
            42.Gibi.bytes to binFactor * binFactor * binFactor * 42.bytes,
            42.Mebi.bytes to binFactor * binFactor * 42.bytes,
            42.Kibi.bytes to binFactor * 42.bytes,
            42.bytes to 42.bytes,

            42.Yotta.bytes to decFactor * decFactor * decFactor * decFactor * decFactor * decFactor * decFactor * decFactor * 42.bytes,
            42.Zetta.bytes to decFactor * decFactor * decFactor * decFactor * decFactor * decFactor * decFactor * 42.bytes,
            42.Exa.bytes to decFactor * decFactor * decFactor * decFactor * decFactor * decFactor * 42.bytes,
            42.Peta.bytes to decFactor * decFactor * decFactor * decFactor * decFactor * 42.bytes,
            42.Tera.bytes to decFactor * decFactor * decFactor * decFactor * 42.bytes,
            42.Giga.bytes to decFactor * decFactor * decFactor * 42.bytes,
            42.Mega.bytes to decFactor * decFactor * 42.bytes,
            42.kilo.bytes to decFactor * 42.bytes,
            42.hecto.bytes to 10 * 10 * 42.bytes, // ⛳️
            42.deca.bytes to 10 * 42.bytes, // 🌽
            42.bytes to 42.bytes,
        ).flatMap { (decimalSize: Size, binarySize: Size) ->
            listOf(
                dynamicTest("$decimalSize == $binarySize") {
                    expectThat(decimalSize).isEqualTo(binarySize)
                },
                dynamicTest("${decimalSize.bytes} == ${binarySize.bytes}") {
                    expectThat(decimalSize.bytes).isEqualTo(binarySize.bytes)
                },
            )
        }
    }
}

