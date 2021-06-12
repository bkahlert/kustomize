package com.imgcstmzr.libguestfs

import com.imgcstmzr.os.DiskDirectory
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.os.boot
import com.imgcstmzr.test.OS
import koodies.docker.DockerRequiring
import koodies.docker.ubuntu
import koodies.exec.ansiRemoved
import koodies.exec.exited
import koodies.exec.io
import koodies.exec.output
import koodies.exec.runtime
import koodies.jvm.thread
import koodies.logging.InMemoryLogger
import koodies.logging.RenderingLogger
import koodies.logging.expectLogged
import koodies.shell.ShellScript
import koodies.test.FifteenMinutesTimeout
import koodies.test.UniqueId
import koodies.test.expecting
import koodies.test.withTempDir
import koodies.text.Banner.banner
import koodies.text.Semantics.formattedAs
import koodies.text.ansiRemoved
import koodies.text.toStringMatchesCurlyPattern
import koodies.time.seconds
import koodies.time.sleep
import koodies.toBaseName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
    fun InMemoryLogger.`should use working script`() {
        val exec = delayScript(3).exec.logging(this)
        expecting { exec } that {
            io.output.ansiRemoved.contains("3") and { contains("FINISHED") }
            exited.runtime.isGreaterThan(3.seconds)
        }
    }

    @Nested
    inner class WaitForEmptyDirectoryScript {

        @Test
        fun `should run until directory is empty`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
            resolve("dir").createDirectory().apply {
                resolve("${fileName}-file1").createFile()
                resolve("${fileName}-file2").createFile()
            }.slowlyDeleteFiles(logger)
            resolve("dir2").createDirectory().apply {
                resolve("${fileName}-file1").createFile()
                resolve("${fileName}-file2").createFile()
            }.slowlyDeleteFiles(logger)
            expecting { ubuntu(logger) { "./${scriptFor("dir", "dir2")}" } } that {
                io.output.ansiRemoved.toStringMatchesCurlyPattern("{{}}CHECKING DIR, DIR2 … {} script(s) to go{{}}")
                io.output.ansiRemoved.containsIgnoringCase("CHECKING DIR, DIR2 … completed")
                io.output.ansiRemoved.containsIgnoringCase("ALL SCRIPTS COMPLETED")
            }
        }

        @Test
        fun `should return if directory is empty`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
            resolve("dir").createDirectory()
            expecting { ubuntu(logger) { "./${scriptFor("dir")}" } } that {
                io.output.ansiRemoved.containsIgnoringCase("CHECKING DIR … completed")
                io.output.ansiRemoved.containsIgnoringCase("ALL SCRIPTS COMPLETED")
            }
        }

        @Test
        fun `should return if directory does not exist`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
            expecting { ubuntu(logger) { "./${scriptFor("dir")}" } } that {
                io.output.ansiRemoved.containsIgnoringCase("CHECKING DIR … completed")
                io.output.ansiRemoved.containsIgnoringCase("ALL SCRIPTS COMPLETED")
            }
        }

        private fun Path.slowlyDeleteFiles(logger: RenderingLogger) =
            thread {
                while (listDirectoryEntries().isNotEmpty()) {
                    3.seconds.sleep()
                    listDirectoryEntries().first().also {
                        logger.logLine { "Deleting ${it.fileName}" }
                    }.deleteIfExists()
                }
            }

        private fun Path.scriptFor(vararg directories: String): Path? =
            FirstBootWait.waitForEmptyDirectory(*directories.map { DiskDirectory(it) }.toTypedArray()).toFile(resolve("script.sh")).fileName
    }

    @FifteenMinutesTimeout @DockerRequiring([DockerPiImage::class]) @Test
    fun InMemoryLogger.`should wait for firstboot scripts to finish`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        osImage.virtCustomize(this) {
            firstBoot(delayScript(30))
        }
        osImage.boot(uniqueId.value.toBaseName(), this)
        expectLogged.ansiRemoved.contains("1 SECONDS TO GO") and {
            contains("FINISHED")
            contains("raspberrypi login:")
        }
    }
}
