package com.imgcstmzr.libguestfs

import com.imgcstmzr.expectRendered
import com.imgcstmzr.os.DiskDirectory
import com.imgcstmzr.os.OS
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.os.boot
import com.imgcstmzr.test.E2E
import koodies.docker.ubuntu
import koodies.exec.RendererProviders
import koodies.exec.ansiRemoved
import koodies.exec.exited
import koodies.exec.io
import koodies.exec.output
import koodies.exec.runtime
import koodies.io.path.deleteDirectoryEntriesRecursively
import koodies.io.path.executable
import koodies.io.path.moveToDirectory
import koodies.junit.UniqueId
import koodies.jvm.thread
import koodies.shell.ShellScript
import koodies.test.Slow
import koodies.test.expecting
import koodies.test.withTempDir
import koodies.text.Banner.banner
import koodies.text.Semantics.formattedAs
import koodies.text.ansiRemoved
import koodies.text.toStringMatchesCurlyPattern
import koodies.time.seconds
import koodies.time.sleep
import koodies.times
import koodies.toBaseName
import koodies.tracing.rendering.BackgroundPrinter
import koodies.tracing.rendering.Styles.None
import koodies.tracing.spanning
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import strikt.assertions.contains
import strikt.assertions.containsIgnoringCase
import strikt.assertions.isEmpty
import strikt.assertions.isGreaterThan
import java.nio.file.Path
import kotlin.io.path.createDirectory
import kotlin.io.path.createFile
import kotlin.io.path.listDirectoryEntries

class FirstBootWaitTest {

    @Nested
    inner class WaitForEmptyDirectoryScript {

        @Slow @Test
        fun `should run until directory is empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val scripts = resolve("dir").createDirectory().apply {
                resolve("${fileName}-file1").createFile().also { it.executable = true }
                resolve("${fileName}-file2").createFile().also { it.executable = true }
            }
            val done = resolve("dir2").createDirectory()
            slowlyProcessFiles(scripts, done)
            expecting { ubuntu(renderer = RendererProviders.block()) { "./${scriptFor("dir", "dir2")}" } } that {
                io.output.ansiRemoved.toStringMatchesCurlyPattern("{{}}CHECKING DIR ⮕ DIR2 … {} SCRIPT(S) TO GO{{}}")
                io.output.ansiRemoved.containsIgnoringCase("CHECKING DIR ⮕ DIR2 … COMPLETED")
            }
        }

        @Slow @Test
        fun `should return if no executable is found`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            val scripts = resolve("dir").createDirectory().apply {
                resolve("${fileName}-file1").createFile()
                resolve("${fileName}-file2").createFile()
            }
            val done = resolve("dir2").createDirectory()
            slowlyProcessFiles(scripts, done)
            expecting { ubuntu(renderer = RendererProviders.block()) { "./${scriptFor("dir", "dir2")}" } } that {
                io.output.ansiRemoved.isEmpty()
            }
        }

        @Test
        fun `should return if directory is empty`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            resolve("dir").createDirectory()
            resolve("dir2").createDirectory()
            expecting { ubuntu(renderer = RendererProviders.block()) { "./${scriptFor("dir", "dir2")}" } } that {
                io.output.ansiRemoved.isEmpty()
            }
        }

        @Test
        fun `should return if directory does not exist`(uniqueId: UniqueId) = withTempDir(uniqueId) {
            resolve("dir2").createDirectory()
            expecting { ubuntu(renderer = RendererProviders.block()) { "./${scriptFor("dir", "dir2")}" } } that {
                io.output.ansiRemoved.isEmpty()
            }
        }

        private fun slowlyProcessFiles(scripts: Path, done: Path) =
            thread {
                spanning("Slowly deleting files", style = None, printer = BackgroundPrinter) {
                    while (scripts.listDirectoryEntries().isNotEmpty()) {
                        3.seconds.sleep()
                        scripts.listDirectoryEntries().first().also {
                            log("Processing ${it.fileName}")
                        }.moveToDirectory(done)
                    }
                    done.deleteDirectoryEntriesRecursively()
                }
            }

        @Suppress("SameParameterValue")
        private fun Path.scriptFor(scripts: String, done: String): Path? =
            FirstBootWait.trackProgress(DiskDirectory(scripts), DiskDirectory(done)).toFile(resolve("script.sh")).fileName
    }

    @Nested
    inner class WithRaspberryPiOS {

        private fun delayScript(seconds: Int) = ShellScript {
            require(seconds > 0) { "Requested seconds ${seconds.formattedAs.input} must be greater than 0." }
            echo(banner("firstboot delay", prefix = ""))
            for (i in seconds downTo 1) {
                echo(banner("$i seconds to go", prefix = ""))
                !"sleep 1"
            }
            echo(banner("finished", prefix = ""))
        }

        @Test
        fun `should use delay script`() {
            val exec = delayScript(3).exec.logging()
            expecting { exec } that {
                io.output.ansiRemoved.contains("3") and { contains("FINISHED") }
                exited.runtime.isGreaterThan(3.seconds)
            }
        }

        @E2E @Test
        fun `should wait for firstboot scripts to finish`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            osImage.virtCustomize {
                5 * { firstBoot(delayScript(5)) }
            }
            osImage.boot(uniqueId.value.toBaseName())
            expectRendered().ansiRemoved {
                contains("CHECKING SCRIPTS ⮕ SCRIPTS-DONE … 5 SCRIPT(S) TO GO")
                contains("CHECKING SCRIPTS ⮕ SCRIPTS-DONE … 4 SCRIPT(S) TO GO")
                contains("CHECKING SCRIPTS ⮕ SCRIPTS-DONE … 3 SCRIPT(S) TO GO")
                contains("CHECKING SCRIPTS ⮕ SCRIPTS-DONE … 2 SCRIPT(S) TO GO")
                contains("CHECKING SCRIPTS ⮕ SCRIPTS-DONE … 1 SCRIPT(S) TO GO")
                contains("1 SECONDS TO GO")
                contains("FINISHED")
                contains("CHECKING SCRIPTS ⮕ SCRIPTS-DONE … COMPLETED")
                contains("raspberrypi login:")
            }
        }
    }
}
