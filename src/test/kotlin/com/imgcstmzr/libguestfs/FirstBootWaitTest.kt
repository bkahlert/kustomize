package com.imgcstmzr.libguestfs

import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.os.boot
import com.imgcstmzr.test.OS
import koodies.docker.DockerRequiring
import koodies.exec.ansiRemoved
import koodies.exec.exited
import koodies.exec.io
import koodies.exec.output
import koodies.exec.runtime
import koodies.logging.InMemoryLogger
import koodies.shell.ShellScript
import koodies.test.FifteenMinutesTimeout
import koodies.test.UniqueId
import koodies.test.expecting
import koodies.text.Banner.banner
import koodies.text.Semantics.formattedAs
import koodies.time.seconds
import koodies.toBaseName
import org.junit.jupiter.api.Test
import strikt.assertions.contains
import strikt.assertions.isGreaterThan

class FirstBootWaitTest {

    private fun delayScript(seconds: Int) = ShellScript {
        require(seconds > 0) { "Requested seconds ${seconds.formattedAs.input} must be greater than 0." }
        !"echo '${banner("firstboot delay")}'"
        for (i in seconds downTo 1) {
            !"echo 'delaying for ${i.formattedAs.input} seconds'"
            !"sleep 1"
        }
        !"echo '${banner("finished")}'"
    }

    @Test
    fun InMemoryLogger.`should use working script`() {
        val exec = delayScript(3).exec.logging(this)
        expecting { exec } that {
            io.output.ansiRemoved.contains("delaying for 3 seconds") and { contains("FINISHED") }
            exited.runtime.isGreaterThan(3.seconds)
        }
    }

    // TODO checken warum beim boot die Zeichen irgendwo bei calculateWidth stecken bleiben

    @FifteenMinutesTimeout @DockerRequiring([DockerPiImage::class]) @Test
    fun InMemoryLogger.`should wait for firstboot scripts to finish`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        osImage.virtCustomize(this) {
            firstBoot(delayScript(10))
        }
        osImage.boot(uniqueId.value.toBaseName(), this)
    }
}
