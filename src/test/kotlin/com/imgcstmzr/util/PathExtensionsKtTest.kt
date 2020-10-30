package com.imgcstmzr.util

import com.bkahlert.koodies.nio.ClassPath
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.test.junit.ConcurrentTestFactory
import com.bkahlert.koodies.test.strikt.hasSize
import com.bkahlert.koodies.unit.bytes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.exists
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.isFalse
import strikt.assertions.isNotNull
import strikt.assertions.isNull
import strikt.assertions.isRegularFile
import strikt.assertions.isTrue
import strikt.assertions.message
import java.io.File
import java.io.IOException
import java.nio.file.Path
import java.time.Instant
import kotlin.time.ExperimentalTime
import kotlin.time.seconds

@Execution(ExecutionMode.CONCURRENT)
internal class PathExtensionsKtTest {

    @Nested
    inner class Exists {

        @Test
        internal fun `should return true if path exists`() {
            val path = File.createTempFile("what", "ever").toPath()
            expectThat(path.exists).isTrue()
        }


        @Test
        internal fun `should return true if classpath exists`() {
            val path = ClassPath.of("config.txt")
            expectThat(path.exists).isTrue()
        }


        @Test
        internal fun `should return false if file is missing exists`() {
            val path = File.createTempFile("what", "ever").toPath()
            path.toFile().delete()
            expectThat(path.exists).isFalse()
        }


        @Test
        internal fun `should return false if classpath is missing`() {
            val path = ClassPath.of("missing.txt")
            expectThat(path.exists).isFalse()
        }
    }

    @Nested
    inner class Executables {

        @Test
        internal fun `should assert isExecutable`() {
            val path = Paths.tempFile()
            expectThat(path.isExecutable).isFalse()

            path.makeExecutable()
            expectThat(path.isExecutable).isTrue()
        }
    }

    @Nested
    inner class FileName {

        @ConcurrentTestFactory
        internal fun `basename of`() = listOf(
            "filename", "filename.test",
            "my/path/filename", "my/path/filename.test",
            "/my/path/filename", "/my/path/filename.test",
        ).flatMap { path ->
            listOf(
                dynamicTest("$path should be filename") {
                    expectThat(Path.of(path).baseName).isEqualTo("filename")
                }
            )
        }

        @ConcurrentTestFactory
        internal fun `extension of`() = listOf(
            "filename" to null,
            "filename." to "",
            "filename.test" to "test",
            "my/path/filename" to null,
            "my/path/filename." to "",
            "my/path/filename.test" to "test",
            "/my/path/filename" to null,
            "/my/path/filename." to "",
            "/my/path/filename.test" to "test",
        ).flatMap { (path, expectedExtension) ->
            listOf(
                dynamicTest("$path should be $expectedExtension") {
                    expectThat(Path.of(path).extension).isEqualTo(expectedExtension)
                }
            )
        }

        @ConcurrentTestFactory
        internal fun `replaced extension of`() = listOf(
            "filename", "filename.test",
            "my/path/filename", "my/path/filename.test",
            "/my/path/filename", "/my/path/filename.test",
        ).flatMap { path ->
            listOf(
                dynamicTest("$path should be filename.test") {
                    expectThat(Path.of(path).fileNameWithExtension("test")).isEqualTo("filename.test")
                }
            )
        }

        @ConcurrentTestFactory
        internal fun `append and remove extension`() = listOf(
            "filename" to "filename.test",
            "my/path/filename" to "my/path/filename.test",
            "/my/path/filename" to "/my/path/filename.test",
            "filename.foo" to "filename.foo.test",
            "my/path/filename.foo" to "my/path/filename.foo.test",
            "/my/path/filename.foo" to "/my/path/filename.foo.test",
        ).flatMap { (path, expected) ->
            listOf(
                dynamicTest("$path with appended extension \"test\" should be $expected") {
                    expectThat(Path.of(path).addExtension("test")).isEqualTo(Path.of(expected))
                },

                dynamicTest("$expected with extension \"test\" removed should be $path") {
                    expectThat(Path.of(expected).removeExtension("test")).isEqualTo(Path.of(path))
                },

                dynamicTest("removing extension \"baz\" from $expected should throw") {
                    expectCatching { Path.of(expected).removeExtension("baz") }.isFailure().isA<IllegalArgumentException>()
                },
            )
        }
    }

    @Nested
    inner class Creation {
        @ExperimentalTime
        @Nested
        inner class Touch {
            val somewhereInThePast = Instant.parse("1984-01-01T00:00:00Z").toEpochMilli()

            @Test
            internal fun `should update modification timestamp`() {
                expectThat(createTempFile().toPath().touch()).lastModified(1.seconds)
            }

            @Test
            internal fun `should throw if file is directory`() {
                expectThat(createTempFile().also { it.delete() }.toPath().mkdirs().touch()).lastModified(1.seconds)
            }

            @Test
            internal fun `should create file if missing`() {
                val path = createTempFile().also { it.delete() }.toPath()
                path.touch()
                expectThat(path).exists()
            }

            @Test
            internal fun `should leave file unchanged if exists`() {
                val path = createTempFile().also { it.writeText("Test") }.toPath()
                path.touch()
                expectThat(path).hasContent("Test")
            }

            @Test
            internal fun `should append text to file`() {
                val path = createTempFile().toPath()
                path.appendText("text 1")
                path.appendText("text 2")
                expectThat(path).hasContent("text 1text 2")
            }

            @Test
            internal fun `should append line to file`() {
                val path = createTempFile().toPath()
                path.appendLine("line 1")
                path.appendLine("line 2")
                expectThat(path).hasContent("line 1\nline 2\n")
            }
        }
    }

    @Nested
    inner class InputStreaming {

        val testFile: Path = ClassPath.of("classpath:/cmdline.txt").copyToTempFile()

        @Suppress("SpellCheckingInspection")
        val expected = "console=serial0,115200 console=tty1 root=PARTUUID=907af7d0-02 rootfstype=ext4 elevator=deadline " +
            "fsck.repair=yes rootwait quiet init=/usr/lib/raspi-config/init_resize.sh\n"

        @ConcurrentTestFactory
        internal fun `should read bytes`(): List<DynamicTest> {
            return listOf(
                ClassPath.of("classpath:/cmdline.txt"),
                testFile,
            ).flatMap { path: Path ->
                listOf(
                    dynamicTest("should stream ${path.quoted}") {
                        expectThat(path.resourceAsStream()).isNotNull().get { String(readAllBytes()) }.isEqualTo(expected)
                    },
                    dynamicTest("should stream buffered ${path.quoted}") {
                        expectThat(path.resourceAsBufferedStream()).isNotNull().get { String(readAllBytes()) }.isEqualTo(expected)
                    },
                    dynamicTest("should read buffered ${path.quoted}") {
                        expectThat(path.resourceAsBufferedReader()).isNotNull().get { readText() }.isEqualTo(expected)
                    },
                )
            }
        }

        @ConcurrentTestFactory
        internal fun `should return null when streaming missing resource`() = listOf(
            ClassPath.of("classpath:/missing.txt"),
            ClassPath.of("/Users/missing/missing.missing"),
        ).flatMap { path: Path ->
            listOf(
                dynamicTest("as stream") {
                    expectThat(path.resourceAsStream()).isNull()
                },

                dynamicTest("as buffered stream") {
                    expectThat(path.resourceAsBufferedStream()).isNull()
                },

                dynamicTest("as buffered reader") {
                    expectThat(path.resourceAsBufferedReader()).isNull()
                },
            )
        }
    }

    @Nested
    inner class Wrap {

        @ConcurrentTestFactory
        @DisplayName("should wrap")
        internal fun `should generate filename_test for`() = listOf(
            "filename" to "@@filename@@",
            "filename.test" to "@@filename.test@@",
            "my/path/filename" to "@@my/path/filename@@",
            "my/path/filename.test" to "@@my/path/filename.test@@",
            "/my/path/filename" to "@@/my/path/filename@@",
            "/my/path/filename.test" to "@@/my/path/filename.test@@",
        ).flatMap { (path, expected) ->
            listOf(
                dynamicTest("$path -> $expected") {
                    val actual = Path.of(path).wrap("@@")
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }
    }

    @Nested
    inner class Quote {

        @ConcurrentTestFactory
        internal fun `should absolute path`() = Paths.WORKING_DIRECTORY.let { pwd ->
            listOf(
                "filename" to "\"$pwd/filename\"",
                "filename.test" to "\"$pwd/filename.test\"",
                "my/path/filename" to "\"$pwd/my/path/filename\"",
                "my/path/filename.test" to "\"$pwd/my/path/filename.test\"",
                "/my/path/filename" to "\"/my/path/filename\"",
                "/my/path/filename.test" to "\"/my/path/filename.test\"",
            )
        }.flatMap { (path, expected) ->
            listOf(
                dynamicTest("$path -> $expected") {
                    val actual = Path.of(path).quoted
                    expectThat(actual).isEqualTo(expected)
                }
            )
        }
    }

    @Nested
    inner class ReadAllBytes {

        val tempFilePrefix = PathExtensionsKtTest::class.simpleName!!
        val tempFile = File.createTempFile(tempFilePrefix, ".txt").also { it.writeBytes(ByteArray(10)); it.deleteOnExit() }

        @ConcurrentTestFactory
        internal fun `should read bytes`() = mapOf(
            tempFile.toPath() to 10,
            ClassPath.of("classpath:funny.img.zip") to 540,
            ClassPath.of("classpath:/cmdline.txt") to 169,
        ).flatMap { (path, expectedSize) ->
            listOf(
                dynamicTest("$expectedSize <- $path") {
                    val actual = path.readAllBytes()
                    expectThat(actual).get { size }.isEqualTo(expectedSize)
                }
            )
        }

        @Test
        internal fun `should throw on missing file`() {
            val deletedFile = File.createTempFile(tempFilePrefix, ".txt").also { it.delete() }.toPath()
            expectCatching { deletedFile.readAllBytes() }.isFailure().isA<IOException>()
        }
    }

    @Nested
    inner class ReadAll {

        @ConcurrentTestFactory
        internal fun `should read complete string`() {
            val expected = """
                console=serial0,115200 console=tty1 root=PARTUUID=907af7d0-02 rootfstype=ext4 elevator=deadline fsck.repair=yes rootwait quiet init=/usr/lib/raspi-config/init_resize.sh

            """.trimIndent()
            expectThat(ClassPath.of("cmdline.txt").readAll()).isEqualTo(expected)
        }

        @Test
        internal fun `should throw on missing file`() {
            expectCatching { ClassPath.of("deleted").readAllBytes() }.isFailure().isA<IOException>()
        }
    }

    @Nested
    inner class Base64 {
        @Test
        internal fun `should encode using Base64`() {
            @Suppress("SpellCheckingInspection")
            val logo =
                """
iVBORw0KGgoAAAANSUhEUgAAAFQAAAARCAMAAAB0IHssAAABYlBMVEVMaXEmq+J4TIfCHnMmq+LCHnMmq+Imq+LCHnMmq+LCHn
Mmq+Imq+Imq+Imq+J2U40mq+Imq+LCHnMmq+LCHnPCHnPCHnPCHnMmq+KNbp04otcmq+Jna5/CHnPCHnPCHnNaiLtqN3ySMHl8b6GSc6HCHnPCHnNilccmq
+JImc1HntKpLnhfhri5LHefToeqSoRtap1yaZ1NndCFeadikMJ8M3pfeqywPX52fKzCHnOEYJWVTIZ3NXtxT4mOMHlqcaN6U4yqKHaRPX9Sir5gf7KCXZKDS
YSxR4JWlcijQoCIWY9vf69lWZGhO32cR4OmOHxrdqhojb6sM3p8iLabVotwY5hlL3gmq+LCHnNYb6SUKXarJHRKjcFiSodkOX1ClcmfJ3W8H3NjQYFyLneDL
Hd+LHdgUox3LXdWd6uaKHVbaJ6JK3axI3SPKnY4nNFdYZkypNlfWpJrL3hPhblUfbKlJXWlLrboAAAAVnRSTlMAMO/AwIDwQECAMBAgsHDr0KAg4NDwoFCQc
MRQ6OCwcND++LA8YJBMYMiI6MSXoHTg2HlkgvvikJQQwMT88/jc5/To6NTP6EyeuMig+tS+0M50zC6U5O6Ej+cAAAAJcEhZcwAAAWIAAAFiAV8n0FMAAAKfS
URBVDiNrdNnU9tAEAbgtXxyb2AwxkCA0Anpvffe27sryb1jesn/z9xZ4CHwicl+0Eg63TOrfeforIqJjNK0yIUzV89ZEREhWyT4P9H7tj1NMdseOy8wC6yL1
B7tbCeJSMTrMicXRkRkdmc7RWTLOtCRzboTSsREZCTAzBUAFU7pW1NDcQrxcQUIgCuyAZRvGXTDYW7XRESA8pxGXQDiAU5Yj0WuMHMRQJGj4WMnQwOTwxq9a
D+sA01bo7V9h3uoioxM6ZVhg/acxedAuY96FeaXrVZrRgU0molGM8w8Meg0lNDog0CI642azlqqcHaBztQwURdwcwZlpnEAS320xPxWRCJEYdMWxbPMUSLS0
8ibmQKYzPAhNsV02oRzB8BVIuIStkbJli2NLgG42UfhcOQESqqP6hFEj9Cys4eOjA4TUQ1w7jaBG/oTB2gVrrWg0fcA/N8HKmuu634MGHSib59CTXkx/VQFJ
nNVIOujKOpLqa2pukFzl4CSWYkaLaTMNANnoo1RrXaA64t/AP3RADXplysGjYz7G4opGqQ/RP+ie0PvDsqoXiCaB+BoppIn4h6a+mA1USqZSfbRcA9wPc8bp
0GnWTqFTsbjIQcNIboHoHug5SzRbh3uiJ9+0EP9COUSvEFQE7Tsx3US/UC0WgLWY5HHAGbU7QbgjEfagDvto2MecJjy0Up5/UT6IWZ1CnWDU9IAPtU2AVSHa
azWADYEwIv+iWImHY85SGth5q6ZKed9NMrMiVOoiFRRrrhbAC4T0cITYF+A9uox+kszGp3RUtugykcTR0kdo88s62swGMx9K6gvn630nHk5/zT9Jvj6lQoQj
QV//FZ5+pm2rJUVy7K+J5RShbSVLqjleFIl40QUVSqltyWVChAR/QWZSglyrdNAqgAAAABJRU5ErkJggg==
""".trimIndent().lines().joinToString("")

            expectThat(ClassPath("BKAHLERT.png").toBase64()).isEqualTo(logo)
        }
    }

    @Nested
    inner class DataUri {
        @Test
        internal fun `should create data URI`() {
            @Suppress("SpellCheckingInspection")
            val logo =
                """
data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAFQAAAARCAMAAAB0IHssAAABYlBMVEVMaXEmq+J4TIfCHnMmq+LCHnMmq+Imq+LCHnMmq+LCHn
Mmq+Imq+Imq+Imq+J2U40mq+Imq+LCHnMmq+LCHnPCHnPCHnPCHnMmq+KNbp04otcmq+Jna5/CHnPCHnPCHnNaiLtqN3ySMHl8b6GSc6HCHnPCHnNilccmq
+JImc1HntKpLnhfhri5LHefToeqSoRtap1yaZ1NndCFeadikMJ8M3pfeqywPX52fKzCHnOEYJWVTIZ3NXtxT4mOMHlqcaN6U4yqKHaRPX9Sir5gf7KCXZKDS
YSxR4JWlcijQoCIWY9vf69lWZGhO32cR4OmOHxrdqhojb6sM3p8iLabVotwY5hlL3gmq+LCHnNYb6SUKXarJHRKjcFiSodkOX1ClcmfJ3W8H3NjQYFyLneDL
Hd+LHdgUox3LXdWd6uaKHVbaJ6JK3axI3SPKnY4nNFdYZkypNlfWpJrL3hPhblUfbKlJXWlLrboAAAAVnRSTlMAMO/AwIDwQECAMBAgsHDr0KAg4NDwoFCQc
MRQ6OCwcND++LA8YJBMYMiI6MSXoHTg2HlkgvvikJQQwMT88/jc5/To6NTP6EyeuMig+tS+0M50zC6U5O6Ej+cAAAAJcEhZcwAAAWIAAAFiAV8n0FMAAAKfS
URBVDiNrdNnU9tAEAbgtXxyb2AwxkCA0Anpvffe27sryb1jesn/z9xZ4CHwicl+0Eg63TOrfeforIqJjNK0yIUzV89ZEREhWyT4P9H7tj1NMdseOy8wC6yL1
B7tbCeJSMTrMicXRkRkdmc7RWTLOtCRzboTSsREZCTAzBUAFU7pW1NDcQrxcQUIgCuyAZRvGXTDYW7XRESA8pxGXQDiAU5Yj0WuMHMRQJGj4WMnQwOTwxq9a
D+sA01bo7V9h3uoioxM6ZVhg/acxedAuY96FeaXrVZrRgU0molGM8w8Meg0lNDog0CI642azlqqcHaBztQwURdwcwZlpnEAS320xPxWRCJEYdMWxbPMUSLS0
8ibmQKYzPAhNsV02oRzB8BVIuIStkbJli2NLgG42UfhcOQESqqP6hFEj9Cys4eOjA4TUQ1w7jaBG/oTB2gVrrWg0fcA/N8HKmuu634MGHSib59CTXkx/VQFJ
nNVIOujKOpLqa2pukFzl4CSWYkaLaTMNANnoo1RrXaA64t/AP3RADXplysGjYz7G4opGqQ/RP+ie0PvDsqoXiCaB+BoppIn4h6a+mA1USqZSfbRcA9wPc8bp
0GnWTqFTsbjIQcNIboHoHug5SzRbh3uiJ9+0EP9COUSvEFQE7Tsx3US/UC0WgLWY5HHAGbU7QbgjEfagDvto2MecJjy0Up5/UT6IWZ1CnWDU9IAPtU2AVSHa
azWADYEwIv+iWImHY85SGth5q6ZKed9NMrMiVOoiFRRrrhbAC4T0cITYF+A9uox+kszGp3RUtugykcTR0kdo88s62swGMx9K6gvn630nHk5/zT9Jvj6lQoQj
QV//FZ5+pm2rJUVy7K+J5RShbSVLqjleFIl40QUVSqltyWVChAR/QWZSglyrdNAqgAAAABJRU5ErkJggg==
""".trimIndent().lines().joinToString("")

            expectThat(ClassPath("BKAHLERT.png").toDataUri()).isEqualTo(logo)
        }
    }

    @Nested
    inner class Copy {

        val tempFilePrefix = PathExtensionsKtTest::class.simpleName!!
        val tempFile = File.createTempFile(tempFilePrefix, ".txt").also { it.writeBytes(ByteArray(10)); it.deleteOnExit() }.toPath()

        @Test
        internal fun `should copy file if destination not exists`() {
            val deletedFile = File.createTempFile(tempFilePrefix, ".txt").also { it.delete() }.toPath()
            tempFile.copyTo(deletedFile)
            expectThat(deletedFile).get { readAllBytes().size }.isEqualTo(10)
        }

        @Test
        internal fun `should throw on missing file`() {
            val deletedFile = File.createTempFile(tempFilePrefix, ".txt").also { it.delete() }.toPath()
            expectCatching { deletedFile.copyTo(tempFile) }.isFailure().isA<IOException>()
        }

        @ConcurrentTestFactory
        internal fun `should create temporary copy for`() = listOf(
            ClassPath.of("cmdline.txt") to Regex(".*/cmdline.*\\.txt"),
            ClassPath.of("cmdline") to Regex(".*/cmdline.*\\.tmp"),
        ).map { (path, matcher) ->
            dynamicContainer("$path",
                listOf(
                    dynamicTest("with matching name") {
                        val tempCopy = path.copyToTempFile()
                        expectThat(tempCopy).absolutePathMatches(matcher)
                    },
                    dynamicTest("with correct content") {
                        val tempCopy = path.copyToTempFile()
                        expectThat(tempCopy).hasEqualContent(ClassPath.of("cmdline.txt"))
                    },
                ))
        }
    }

    @Nested
    inner class Move {

        val tempFilePrefix = PathExtensionsKtTest::class.simpleName!!
        fun tempFile() = File.createTempFile(tempFilePrefix, ".txt").also { it.writeBytes(ByteArray(10)); it.deleteOnExit() }.toPath()

        @Test
        internal fun `should copy file if destination not exists`() {
            val deletedFile = File.createTempFile(tempFilePrefix, ".txt").also { it.delete() }.toPath()
            val tempFile = tempFile()
            tempFile.moveTo(deletedFile)
            expectThat(deletedFile).get { readAllBytes().size }.isEqualTo(10)
            expectThat(tempFile).not { exists() }
        }

        @Test
        internal fun `should throw on missing file`() {
            val deletedFile = File.createTempFile(tempFilePrefix, ".txt").also { it.delete() }.toPath()
            expectCatching { deletedFile.moveTo(tempFile()) }.isFailure().isA<IOException>()
        }
    }

    @Nested
    inner class Rename {

        val tempFilePrefix = PathExtensionsKtTest::class.simpleName!!
        val tempFile = File.createTempFile(tempFilePrefix, ".txt").also { it.writeBytes(ByteArray(10)); it.deleteOnExit() }.toPath()

        @Test
        internal fun `should rename file if destination not exists`() {
            val renamedFilename = String.random()
            val renamedFile = tempFile.renameTo("${renamedFilename}.txt")
            expectThat(tempFile).not { exists() }
            expectThat(renamedFile).exists().get { readAllBytes().size }.isEqualTo(10)
        }

        @Test
        internal fun `should throw on missing file`() {
            val deletedFile = File.createTempFile(tempFilePrefix, ".txt").also { it.delete() }.toPath()
            expectCatching { deletedFile.renameTo("filename") }.isFailure().isA<IllegalArgumentException>()
        }

        @Test
        internal fun `should throw on classpath file`() {
            expectCatching { ClassPath("cmdline.txt").renameTo("filename") }.isFailure().isA<IllegalArgumentException>()
        }
    }

    @Nested
    inner class Cleanup {

        val tempFilePrefix = PathExtensionsKtTest::class.simpleName!!
        fun getTempFile(additionalSuffix: String) = File.createTempFile(tempFilePrefix, ".txt$additionalSuffix")
            .also { it.writeBytes(ByteArray(10)); it.deleteOnExit() }.toPath()

        @Test
        internal fun `should rename file if trailing dirt`() {
            val file = getTempFile("?x")
            val cleanedFile = file.cleanUp("?")
            expectThat(file).not { exists() }
            expectThat(cleanedFile)
                .exists()
                .hasSize(10.bytes)
                .get { fileName.toString() }.not { contains("?") }
        }

        @Test
        internal fun `should not rename file without trailing dirt`() {
            val file = getTempFile("?x")
            val cleanedFile = file.cleanUp("#")
            expectThat(cleanedFile).exists().isEqualTo(file)
        }
    }

    @Nested
    inner class Temp {

        val prefix = String.random(4)

        @Test
        internal fun `should create empty temp file`() {
            val path = File.createTempFile(prefix, ".txt").also { it.writeText("Test") }.toPath()
            expectThat(path.asEmptyTempFile())
                .isRegularFile()
                .hasContent("")
                .isInside(path.parent)
        }

        @Test
        internal fun `should create empty temp file in isolated directory`() {
            val path = File.createTempFile(prefix, ".txt").also { it.writeText("Test") }.toPath()
            val temp = path.asEmptyTempFile(isolated = true)
            expectThat(temp)
                .isRegularFile()
                .hasContent("")
                .isInside(temp.parent)
                .and { temp.delete() }
                .get { parent }
                .isEmptyDirectory()
        }

        @Test
        internal fun `should create temp copy`() {
            val path = File.createTempFile(prefix, ".txt").also { it.writeText("Test") }.toPath()
            val tempCopy = path.copyToTempFile()
            expectThat(tempCopy)
                .isRegularFile()
                .hasContent("Test")
                .isInside(path.parent)
        }

        @Test
        internal fun `should create temp copy in isolated directory`() {
            val path = File.createTempFile(prefix, ".txt").also { it.writeText("Test") }.toPath()
            val tempCopy = path.copyToTempFile(isolated = true)
            expectThat(tempCopy)
                .isRegularFile()
                .hasContent("Test")
                .isInside(path.parent)
                .and { tempCopy.delete() }
                .get { parent }
                .isEmptyDirectory()
        }

        @Test
        internal fun `should create temp sibling`() {
            val path = File.createTempFile(prefix, ".txt").also { it.writeText("Test") }.toPath()
            val tempSiblingCopy = path.copyToTempSiblingDirectory()
            expectThat(tempSiblingCopy)
                .isRegularFile()
                .hasContent("Test")
                .isInside(path.parent.parent)
                .and { tempSiblingCopy.delete() }
                .get { parent }
                .isEmptyDirectory()
        }
    }

    @Nested
    inner class CheckSingleFile {
        @Test
        internal fun `should return single file`() {
            val dir = Paths.tempFile().copyToTempSiblingDirectory().parent
            expectThat(dir.checkSingleFile { "File not found." }).isInside(dir)
        }

        @Test
        internal fun `should throw on missing file`() {
            val dir = Paths.tempFile().copyToTempSiblingDirectory().also { it.delete() }.parent
            expectCatching { dir.checkSingleFile { "File not found." } }.isFailure().isA<IllegalStateException>()
                .message.assert("") { expectThat(it).isEqualTo("File not found.") }
        }

        @Test
        internal fun `should throw on too many files`() {
            val dir = Paths.tempFile().copyToTempSiblingDirectory().also { it.parent.resolve("second.file").touch() }.parent
            expectCatching { dir.checkSingleFile { "File not found." } }.isFailure().isA<IllegalStateException>()
                .message.assert("") { expectThat(it).isEqualTo("File not found.") }
        }
    }
}

inline operator fun <reified T : Number> T.times(function: (Int) -> Unit) {
    (0 until toInt()).forEach { i -> function(i) }
}
