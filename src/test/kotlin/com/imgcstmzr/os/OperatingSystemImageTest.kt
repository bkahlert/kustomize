package com.imgcstmzr.os

import com.imgcstmzr.ImgCstmzr
import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.os.OperatingSystemImage.Companion.based
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.OS
import koodies.content
import koodies.docker.DockerRequiring
import koodies.io.path.hasContent
import koodies.io.path.isDuplicateOf
import koodies.io.path.pathString
import koodies.io.path.touch
import koodies.io.path.withDirectoriesCreated
import koodies.io.path.writeLine
import koodies.io.path.writeText
import koodies.io.randomDirectory
import koodies.io.randomFile
import koodies.junit.UniqueId
import koodies.test.CapturedOutput
import koodies.test.FifteenMinutesTimeout
import koodies.test.FiveMinutesTimeout
import koodies.test.Smoke
import koodies.test.SystemIOExclusive
import koodies.test.toStringContainsAll
import koodies.test.withTempDir
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
import kotlin.io.path.readLines

@SystemIOExclusive
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
        expectThat((OperatingSystemMock("full-name-test") based Path.of("foo/bar")).fullName)
            .isEqualTo("ImgCstmzr Test OS ／ file://${ImgCstmzr.WorkingDirectory}/foo/bar")
    }

    @Test
    fun `should have short name`() {
        expectThat((OperatingSystemMock("short-name-test") based Path.of("foo/bar")).shortName)
            .isEqualTo("ImgCstmzr Test OS ／ bar")
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

            expectThat(osImage.exchangeDirectory.resolve("boot/cmdline.txt"))
                .content.toStringContainsAll("console=serial", "console=tty", "rootfstype=ext4")
        }
    }

    @Nested
    inner class Guestfish {

        @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
        fun `should trace if specified`(
            @OS(RaspberryPiLite) osImage: OperatingSystemImage,
            output: CapturedOutput,
        ) {
            osImage.guestfish(true) {}

            expectThat(output.all).contains("libguestfs: trace: launch")
        }

        @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
        fun `should copy-out existing file`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            osImage.guestfish {
                copyOut { LinuxRoot.boot / "cmdline.txt" }
            }

            expectThat(osImage.exchangeDirectory.resolve("boot/cmdline.txt"))
                .content.toStringContainsAll("console=serial", "console=tty", "rootfstype=ext4")
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
            val dir = osImage.exchangeDirectory.apply { resolve("boot/cmdline.txt").withDirectoriesCreated().writeText("overwrite me") }
            osImage.guestfish {
                copyOut { LinuxRoot / "boot" / "cmdline.txt" }
            }

            expectThat(dir.resolve("boot/cmdline.txt")).content
                .not { contains("overwrite me") }
                .contains("console=serial")
        }

        @FiveMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Test
        fun `should override existing file`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            val dir = osImage.exchangeDirectory.apply { resolve("boot/cmdline.txt").withDirectoriesCreated().writeText("overwrite me") }
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
    inner class VirtCustomize {

        @FifteenMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @Smoke @Test
        fun `should set hostname`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            osImage.virtCustomize {
                hostname { "test-machine" }
            }
            expectThat(osImage).mounted {
                path("/etc/hostname") {
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
