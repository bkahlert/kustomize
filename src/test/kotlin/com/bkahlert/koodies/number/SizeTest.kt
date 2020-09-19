package com.bkahlert.koodies.number

import com.bkahlert.koodies.test.strikt.matches
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
import com.imgcstmzr.util.logging.AdHocOutputCapture.Companion.capture
import com.imgcstmzr.util.times
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.parallel.Isolated
import strikt.api.expectThat
import strikt.assertions.isEqualTo

@Execution(ExecutionMode.CONCURRENT)
@Suppress("RedundantInnerClassModifier")
internal class SizeTest {

    @TestFactory
    internal fun `should format decimal format`() = listOf(
        42.Yotta.bytes to "42 YB",
        42.Zetta.bytes to "42 ZB",
        42.Exa.bytes to "42 EB",
        42.Peta.bytes to "42 PB",
        42.Tera.bytes to "42 TB",
        42.Giga.bytes to "42 GB",
        42.Mega.bytes to "42 MB",
        42.kilo.bytes to "42 kB",
        42.bytes to "42 B",
    ).map { (size: Size, expected: String) ->
        dynamicTest("$size -> $expected") {
            expectThat(size.formatted).isEqualTo(expected)
        }
    }

    @TestFactory
    internal fun `should not use untypical decimal prefixes for decimal format`() = listOf(
        42.hecto.bytes to "4 kB",
        42.deca.bytes to "420 B",
        42.deci.bytes to "0 B",
        42.centi.bytes to "0 B",
        42.milli.bytes to "0 B",
        42.micro.bytes to "0 B",
        42.nano.bytes to "0 B",
        42.pico.bytes to "0 B",
        42.femto.bytes to "0 B",
        42.atto.bytes to "0 B",
        42.zebi_.bytes to "0 B",
        42.yobi_.bytes to "0 B",
    ).map { (size: Size, expected: String) ->
        dynamicTest("$size -> $expected") {
            expectThat(size.formatted).isEqualTo(expected)
        }
    }

    @TestFactory
    internal fun `should format binary format`() = listOf(
        42.Yobi.bytes to "42 YiB",
        42.Zebi.bytes to "42 ZiB",
        42.Exbi.bytes to "42 EiB",
        42.Pebi.bytes to "42 PiB",
        42.Tebi.bytes to "42 TiB",
        42.Gibi.bytes to "42 GiB",
        42.Mebi.bytes to "42 MiB",
        42.Kibi.bytes to "42 KiB",
        42.bytes to "42 B",
    ).map { (size: Size, expected: String) ->
        dynamicTest("$size -> $expected") {
            expectThat(size.binaryFormatted).isEqualTo(expected)
        }
    }

    @TestFactory
    internal fun `should not use untypical prefixes for binary format`() = listOf(
        42.mibi.bytes to "0 B",
        42.mubi.bytes to "0 B",
        42.nabi.bytes to "0 B",
        42.pibi.bytes to "0 B",
        42.fembi.bytes to "0 B",
        42.abi.bytes to "0 B",
        42.zebi_.bytes to "0 B",
        42.yobi_.bytes to "0 B",
    ).map { (size: Size, expected: String) ->
        dynamicTest("$size -> $expected") {
            expectThat(size.formatted).isEqualTo(expected)
        }
    }

    @Nested
    @Isolated
    inner class Unsupported {
        @TestFactory
        internal fun `should warn of unsupported decimal prefixes`() = listOf(
            42::deci to DecimalPrefix.deci,
            42::centi to DecimalPrefix.centi,
            42::milli to DecimalPrefix.milli,
            42::micro to DecimalPrefix.micro,
            42::nano to DecimalPrefix.nano,
            42::pico to DecimalPrefix.pico,
            42::femto to DecimalPrefix.femto,
            42::atto to DecimalPrefix.atto,
            42::zepto to DecimalPrefix.zepto,
            42::yocto to DecimalPrefix.yocto,
        ).map { (property, prefix) ->
            dynamicTest("$prefix") {
                expectThat(capture { prefix.getValue(42, property) }).matches("{} currently not {} supported{}")
            }
        }

        @TestFactory
        internal fun `should warn of unsupported binary prefixes`() = listOf(
            42::mibi to BinaryPrefix.mibi,
            42::mubi to BinaryPrefix.mubi,
            42::nabi to BinaryPrefix.nabi,
            42::pibi to BinaryPrefix.pibi,
            42::fembi to BinaryPrefix.fembi,
            42::abi to BinaryPrefix.abi,
            42::zebi_ to BinaryPrefix.zebi,
            42::yobi_ to BinaryPrefix.yobi,
        ).map { (property, prefix) ->
            dynamicTest("$prefix") {
                expectThat(capture { prefix.getValue(42, property) }).matches("{} currently not {} supported{}")
            }
        }
    }


    @Nested
    inner class AsSize {

        val tempFile = Paths.tempFile().apply { 2500.times { appendText("1234567890") } }

        @Test
        internal fun `should format size human-readable (power of 1000)`() {
            expectThat(tempFile.size.formatted).isEqualTo("25 kB")
        }

        @Test
        internal fun `should format size human-readable (power of 1024)`() {
            expectThat(tempFile.size.binaryFormatted).isEqualTo("24 KiB")
        }
    }
}
