package com.imgcstmzr.patch.ini

import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.imgcstmzr.patch.ini.IniDocument.CommentLine
import com.imgcstmzr.patch.ini.IniDocument.KeyLine
import com.imgcstmzr.patch.ini.IniDocument.Line
import com.imgcstmzr.patch.ini.IniDocument.SectionLine
import com.imgcstmzr.util.copyToTempFile
import com.imgcstmzr.util.quoted
import com.imgcstmzr.util.readAll
import com.imgcstmzr.util.replaceNonPrintableCharacters
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.endsWith
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEmpty
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isNull

@Execution(ExecutionMode.CONCURRENT)
internal class IniDocumentTest {

    @Suppress("SpellCheckingInspection") val rawDocument = """
        # http://rpf.io/configtxt
        # Some settings may impact device functionality. See link above for details
    
        #uncomment to overclock the arm. 700 MHz is the default.
        #arm_freq=800
    
        # Uncomment this to enable infrared communication.
        #dtoverlay=gpio-ir,gpio_pin=17
        #dtoverlay=gpio-ir-tx,gpio_pin=18
    
        # Additional overlays and parameters are documented /boot/overlays/README
    
        # Enable audio (loads snd_bcm2835)
        dtparam=audio=on
    
        [pi4]
        # Enable DRM VC4 V3D driver on top of the dispmanx display stack
        dtoverlay=vc4-fkms-v3d
        max_framebuffers=2
    
        [all]
        #dtoverlay=vc4-fkms-v3d
        dtoverlay=dwc2
    
           abc = 'de f"g${"\t\t\t"} #t${'$'}€$£💷 peace ✌️
        """.trimIndent()

    @ConcurrentTestFactory
    internal fun fragment(): List<DynamicTest> = listOf(
        """
            dtparam=audio=on
        """.trimIndent() to listOf(::KeyLine),
        """
            # http://rpf.io/configtxt
        """.trimIndent() to listOf(::CommentLine),
        """
            abc = 'de f"g
        """.trimIndent() to listOf(::KeyLine),
        """
            #dtoverlay=vc4-fkms-v3d 
        """.trimIndent() to listOf(::CommentLine),
        """
            #t${'$'}€${'$'}£💷 peace ✌️
        """.trimIndent() to listOf(::CommentLine),
        """
            abc = 'de f"g${"\t\t\t"} #t${'$'}€${'$'}£💷 peace ✌️ 
        """.trimIndent() to listOf(::Line),

        ).flatMap { (fragment: String, parsers: List<(String) -> Any>) ->
        listOf(::SectionLine, ::KeyLine, ::CommentLine)
            .filter { !parsers.contains(it) }
            .map { parser ->
                dynamicTest("${fragment.replaceNonPrintableCharacters().quoted} should NOT be parsed by $parser") {
                    expectCatching {
                        val instance = parser(fragment)
                        println(instance)
                    }.assert("") { result ->
                        println(result)
                    }.isFailure().isA<IllegalArgumentException>()
                }
            }.plus(
                parsers.map { parser ->
                    dynamicTest("${fragment.replaceNonPrintableCharacters().quoted} should be parsed by $parser") {
                        val actual = parser(fragment)
                        println(actual)
                        expectThat(actual.toString()).isEqualTo(fragment)
                    }
                })
    }

    @Nested
    inner class Loading {

        @Test
        fun `should work`() {
            val iniDocument = IniDocument(rawDocument)
            expectThat(iniDocument).hasSize(25)
        }

        @Test
        fun `should find section range`() {
            val iniDocument = IniDocument(rawDocument)
            expectThat(iniDocument.findSection("pi4")).isEqualTo(15..20)
        }

        @Test
        fun `should find section range if last section`() {
            val iniDocument = IniDocument(rawDocument)
            expectThat(iniDocument.findSection("all")).isEqualTo(20..25)
        }

        @Test
        fun `should find section range if unnamed section`() {
            val iniDocument = IniDocument(rawDocument)
            expectThat(iniDocument.findSection(null)).isEqualTo(20..25)
        }

        @Test
        fun `should return null if missing section`() {
            val iniDocument = IniDocument(rawDocument)
            expectThat(iniDocument.findSection("???")).isNull()
        }

        @Test
        fun `should find key by name`() {
            val iniDocument = IniDocument(rawDocument)
            val foundKeys = iniDocument.findKey("dtparam")
            expectThat(foundKeys).hasSize(1).get { get(0).toString() }.isEqualTo("dtparam=audio=on")
        }

        @Suppress("SpellCheckingInspection")
        @Test
        fun `should find key by name and section`() {
            val iniDocument = IniDocument(rawDocument)
            expectThat(iniDocument.findKey("dtoverlay", null)).hasSize(2)
                .get { get(0).toString() to get(1).toString() }
                .isEqualTo("dtoverlay=vc4-fkms-v3d" to "dtoverlay=dwc2")
            expectThat(iniDocument.findKey("dtoverlay", "all"))
                .hasSize(1).get { get(0).toString() }
                .isEqualTo("dtoverlay=dwc2")
        }

        @Test
        fun `should return empty on missing key`() {
            val iniDocument = IniDocument(rawDocument)
            val foundKeys = iniDocument.findKey("???")
            expectThat(foundKeys).isEmpty()
        }

        @Suppress("SpellCheckingInspection")
        @Test
        fun `should return empty on missing section`() {
            val iniDocument = IniDocument(rawDocument)
            val foundKeys = iniDocument.findKey("dtoverlay", "???")
            expectThat(foundKeys).isEmpty()
        }

        @Test
        fun `should edit key`() {
            val iniDocument = IniDocument(rawDocument)
            val foundKeys = iniDocument.findKey("dtparam")
            foundKeys.onEach { it.values = listOf("new-value") }
            expectThat(foundKeys).hasSize(1).get { get(0).toString() }.isEqualTo("dtparam=new-value")
            println(iniDocument)
        }

        @Suppress("SpellCheckingInspection")
        @Test
        fun `should add to existing key`() {
            val iniDocument = IniDocument(rawDocument)
            val foundKeys = iniDocument.findKey("dtoverlay")
            foundKeys.onEach { it.values += "added-value" }
            expectThat(iniDocument.toString())
                .contains("dtoverlay=vc4-fkms-v3d,added-value")
                .contains("dtoverlay=dwc2,added-value")
        }

        @Test
        fun `should add missing key`() {
            val iniDocument = IniDocument(rawDocument)
            iniDocument.findKey("missing").also { expectThat(it).isEmpty() }
            iniDocument.append("missing=no-more")
            expectThat(iniDocument.toString()).endsWith("missing=no-more")
            println(iniDocument.toString())
        }

        @Test
        fun `should create if missing`() {
            val iniDocument = IniDocument(rawDocument)

            iniDocument.createKeyIfMissing("missing", "v1", "all")
            iniDocument.createKeyIfMissing("missing", "v2", "all")
            iniDocument.createKeyIfMissing("missing", "v3", "xxx")

            expectThat(iniDocument.toString()).endsWith("missing=v1,v2\n[xxx]\nmissing=v3")
            println(iniDocument.toString())
        }

        @Suppress("SpellCheckingInspection")
        @Test
        fun `should make basic changes and persist file`() {
            val path = ClassPath.of("config.txt").copyToTempFile()
            val iniDocument = IniDocument(path)

            iniDocument.findKey("dtoverlay").onEach { it.values += "added-value" }
            iniDocument.save(path)

            expectThat(path.readAll())
                .contains("dtoverlay=vc4-fkms-v3d,added-value")
                .contains("dtoverlay=dwc2,added-value")
            println(iniDocument)
        }
    }
}
