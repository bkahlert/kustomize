package com.bkahlert.kustomize.os

import com.bkahlert.kustomize.Kustomize
import com.bkahlert.kustomize.expectRendered
import com.bkahlert.kustomize.libguestfs.LibguestfsImage
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.OperatingSystemImage.Companion.based
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import koodies.docker.DockerRequiring
import koodies.io.createParentDirectories
import koodies.io.path.hasContent
import koodies.io.path.isDuplicateOf
import koodies.io.path.textContent
import koodies.io.path.touch
import koodies.io.path.writeLine
import koodies.io.path.writeText
import koodies.io.randomDirectory
import koodies.io.randomFile
import koodies.junit.UniqueId
import koodies.test.FifteenMinutesTimeout
import koodies.test.FiveMinutesTimeout
import koodies.test.Smoke
import koodies.test.toStringContainsAll
import koodies.test.withTempDir
import koodies.unit.Gibi
import koodies.unit.bytes
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.Assertion
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.isEqualTo
import strikt.java.exists
import java.nio.file.Path
import kotlin.io.path.div
import kotlin.io.path.isWritable
import kotlin.io.path.pathString
import kotlin.io.path.readLines

class OperatingSystemImageTest {

    @Test
    fun `should have correct absolute path`() {
        expectThat((OperatingSystemMock("abs-path-test") based Path.of("/foo/bar"))).serializedPathIsEqualTo("/foo/bar")
    }

    @Test
    fun `should have correct relative path`() {
        expectThat((OperatingSystemMock("rel-path-test") based Path.of("foo/bar"))).serializedPathIsEqualTo("foo/bar")
    }

    @Test
    fun `should have full name`() {
        @Suppress("SpellCheckingInspection")
        expectThat((OperatingSystemMock("full-name-test") based Path.of("foo/bar")).fullName)
            .isEqualTo("RISC OS Pico RC5 (test only) ／ file://${Kustomize.WorkingDirectory}/foo/bar")
    }

    @Test
    fun `should have short name`() {
        @Suppress("SpellCheckingInspection")
        expectThat((OperatingSystemMock("short-name-test") based Path.of("foo/bar")).shortName)
            .isEqualTo("RISC OS Pico RC5 (test only) ／ bar")
    }

    @Test
    fun `should duplicate`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val osImage = OperatingSystemImage(OperatingSystemMock(uniqueId.value), path = randomDirectory().randomFile(extension = ".img"))
        expectThat(osImage.duplicate()).path.isDuplicateOf(osImage.file)
    }

    @Nested
    inner class CopyOut {

        @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Smoke @Test
        fun `should copy-out existing file`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            osImage.copyOut("/boot/cmdline.txt")

            @Suppress("SpellCheckingInspection")
            expectThat(osImage.exchangeDirectory.resolve("boot/cmdline.txt"))
                .textContent.toStringContainsAll("console=serial", "console=tty", "rootfstype=ext4")
        }
    }

    @Nested
    inner class Guestfish {

        @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
        fun `should trace if specified`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            osImage.guestfish(true) {}
            expectRendered().contains("libguestfs: trace: launch")
        }

        @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
        fun `should copy-out existing file`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            osImage.guestfish {
                copyOut { LinuxRoot.boot / "cmdline.txt" }
            }

            @Suppress("SpellCheckingInspection")
            expectThat(osImage.exchangeDirectory.resolve("boot/cmdline.txt"))
                .textContent.toStringContainsAll("console=serial", "console=tty", "rootfstype=ext4")
        }

        @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
        fun `should ignore missing files`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            osImage.guestfish {
                copyOut { LinuxRoot / "missing.txt" }
            }

            expectThat(osImage.exchangeDirectory / "missing.txt") {
                not { exists() }
            }
        }

        @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
        fun `should override locally existing file`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            val dir = osImage.exchangeDirectory.apply { resolve("boot/cmdline.txt").createParentDirectories().writeText("overwrite me") }
            osImage.guestfish {
                copyOut { LinuxRoot / "boot" / "cmdline.txt" }
            }

            expectThat(dir.resolve("boot/cmdline.txt")).textContent {
                not { contains("overwrite me") }
                contains("console=serial")
            }
        }

        @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
        fun `should override existing file`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            val dir = osImage.exchangeDirectory.apply { resolve("boot/cmdline.txt").createParentDirectories().writeText("overwrite me") }
            osImage.guestfish {
                copyIn { LinuxRoot / "boot" / "cmdline.txt" }
            }
            osImage.guestfish {
                copyOut { LinuxRoot / "boot" / "cmdline.txt" }
            }

            expectThat(dir.resolve("boot/cmdline.txt")).hasContent("overwrite me")
        }
    }

    @Nested
    inner class Resize {

        @FifteenMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
        fun `should resize`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            val targetSize = 4.Gibi.bytes.also { check(osImage.size < it) { "$osImage is already larger than $it. Please update test." } }
            expectThat(osImage.resize(targetSize)).isEqualTo(targetSize)
        }
    }

    @Nested
    inner class VirtCustomize {

        @FifteenMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Smoke @Test
        fun `should set hostname`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            osImage.virtCustomize {
                hostname { "test-machine" }
            }
            expectThat(osImage).mounted {
                path(LinuxRoot.etc.hostname) {
                    exists()
                    hasContent("test-machine\n")
                }
            }
        }
    }
}

val Assertion.Builder<OperatingSystemImage>.size get() = get("size") { size }
val Assertion.Builder<OperatingSystemImage>.path get() = get("path") { file }
fun Assertion.Builder<OperatingSystemImage>.serializedPathIsEqualTo(path: String) =
    assert("path equals %s") {
        val actual = it.file.pathString
        if (actual == path) pass()
        else fail("is $actual")
    }

fun <T : Path> Assertion.Builder<T>.canWriteAndRead() =
    assert("can write and read %s") {
        it.touch()
        if (!it.isWritable()) fail("is not writable")
        it.writeLine("write assertion")
        val lines = it.readLines()
        if (lines.first() != "write assertion") fail("content was $lines instead of \"write assertion\"")
        pass()
    }