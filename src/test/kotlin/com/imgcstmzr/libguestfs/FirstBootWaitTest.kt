package com.imgcstmzr.libguestfs

import com.imgcstmzr.os.DiskDirectory
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.os.boot
import com.imgcstmzr.test.OS
import koodies.docker.DockerRequiring
import koodies.docker.ubuntu
import koodies.exec.RendererProviders
import koodies.exec.ansiRemoved
import koodies.exec.exited
import koodies.exec.io
import koodies.exec.output
import koodies.exec.runtime
import koodies.io.path.executable
import koodies.junit.UniqueId
import koodies.jvm.thread
import koodies.shell.ShellScript
import koodies.test.CapturedOutput
import koodies.test.FifteenMinutesTimeout
import koodies.test.Slow
import koodies.test.SystemIOExclusive
import koodies.test.expecting
import koodies.test.withTempDir
import koodies.text.Banner.banner
import koodies.text.Semantics.formattedAs
import koodies.text.ansiRemoved
import koodies.text.toStringMatchesCurlyPattern
import koodies.time.seconds
import koodies.time.sleep
import koodies.toBaseName
import koodies.tracing.rendering.BackgroundPrinter
import koodies.tracing.rendering.Styles.None
import koodies.tracing.spanning
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsIgnoringCase
import strikt.assertions.isGreaterThan
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.deleteIfExists
import kotlin.io.path.listDirectoryEntries

class FirstBootWaitTest {

    private fun delayScript(seconds: Int) = ShellScript {
        require(seconds > 0) { "Requested seconds ${seconds.formattedAs.input} must be greater than 0." }
        echo(banner("firstboot delay"))
        for (i in seconds downTo 1) {
            echo(banner("$i seconds to go"))
            !"sleep 1"
        }
        echo(banner("finished"))
    }

    @Test
    fun `should use working script`() {
        val exec = delayScript(3).exec.logging()
        expecting { exec } that {
            io.output.ansiRemoved.contains("3") and { contains("FINISHED") }
            exited.runtime.isGreaterThan(3.seconds)
        }
    }

    @Nested
    inner class WaitForEmptyDirectoryScript {

        @Slow @Test
        fun `should run until directory is empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            resolve("dir").createDirectory().apply {
                resolve("${fileName}-file1").createFile().also { it.executable = true }
                resolve("${fileName}-file2").createFile().also { it.executable = true }
            }.slowlyDeleteFiles()
            resolve("dir2").createDirectory().apply {
                resolve("${fileName}-file1").createFile().also { it.executable = true }
                resolve("${fileName}-file2").createFile().also { it.executable = true }
            }.slowlyDeleteFiles()
            expecting { ubuntu(RendererProviders.block()) { "./${scriptFor("dir", "dir2")}" } } that {
                io.output.ansiRemoved.toStringMatchesCurlyPattern("{{}}CHECKING DIR, DIR2 … {} script(s) to go{{}}")
                io.output.ansiRemoved.containsIgnoringCase("CHECKING DIR, DIR2 … completed")
                io.output.ansiRemoved.containsIgnoringCase("ALL SCRIPTS COMPLETED")
            }
        }

        @Slow @Test
        fun `should return if no executable is found`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            resolve("dir").createDirectory().apply {
                resolve("${fileName}-file1").createFile()
            }
            expecting { ubuntu(RendererProviders.block()) { "./${scriptFor("dir")}" } } that {
                io.output.ansiRemoved.not { contains("script(s) to go") }
                io.output.ansiRemoved.containsIgnoringCase("CHECKING DIR … completed")
                io.output.ansiRemoved.containsIgnoringCase("ALL SCRIPTS COMPLETED")
            }
        }

        @Test
        fun `should return if directory is empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            resolve("dir").createDirectory()
            expecting { ubuntu { "./${scriptFor("dir")}" } } that {
                io.output.ansiRemoved.containsIgnoringCase("CHECKING DIR … completed")
                io.output.ansiRemoved.containsIgnoringCase("ALL SCRIPTS COMPLETED")
            }
        }

        @Test
        fun `should return if directory does not exist`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            expecting { ubuntu { "./${scriptFor("dir")}" } } that {
                io.output.ansiRemoved.containsIgnoringCase("CHECKING DIR … completed")
                io.output.ansiRemoved.containsIgnoringCase("ALL SCRIPTS COMPLETED")
            }
        }

        private fun Path.slowlyDeleteFiles() =
            thread {
                spanning("Slowly deleting files", style = None, printer = BackgroundPrinter) {
                    while (listDirectoryEntries().isNotEmpty()) {
                        3.seconds.sleep()
                        listDirectoryEntries().first().also {
                            log("Deleting ${it.fileName}")
                        }.deleteIfExists()
                    }
                }
            }

        private fun Path.scriptFor(vararg directories: String): Path? =
            FirstBootWait.waitForEmptyDirectory(*directories.map { DiskDirectory(it) }.toTypedArray()).toFile(resolve("script.sh")).fileName
    }

    @SystemIOExclusive
    @FifteenMinutesTimeout @DockerRequiring([DockerPiImage::class]) @Test
    fun `should wait for firstboot scripts to finish`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage, output: CapturedOutput) {
        osImage.virtCustomize {
            firstBoot(delayScript(30))
        }
        osImage.boot(uniqueId.value.toBaseName())
        expectThat(output.all).ansiRemoved
            .contains("1 SECONDS TO GO")
            .contains("FINISHED")
            .contains("raspberrypi login:")
    }
}
