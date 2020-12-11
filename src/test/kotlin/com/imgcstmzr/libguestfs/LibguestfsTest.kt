package com.imgcstmzr.libguestfs

import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.test.junit.FiveMinutesTimeout
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.guestfish.mounted
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.Companion.customize
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.util.DockerRequiring
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.hasContent
import com.imgcstmzr.util.logging.InMemoryLogger
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.exists
import java.nio.file.Path

@Execution(CONCURRENT)
class LibguestfsTest {

    @Nested
    inner class ForGuestfish {
        @Test
        fun `should build proper docker command`() {
            val osImage: Path = Path.of("/Users/bkahlert/.imgcstmzr.test/guestfish/disk.img")
            val command = GuestfishCommandLine.Companion.build {
                options {
                    disks { +osImage }
                    inspector { on }
                }

                commands {
                    ignoreErrors {
                        copyIn(Path.of("/home/pi/.ssh/known_hosts"))
                    }
                    copyOut(Path.of("/home/pi/.ssh/known_hosts"))
                }
            }

            expectThat(command.adapt().toString()).matchesCurlyPattern("""
            docker \
            run \
            --entrypoint \
            guestfish \
            --name \
            libguestfs-guestfish-{} \
            --rm \
            -i \
            --mount \
            type=bind,source={}/shared,target=/shared \
            --mount \
            type=bind,source={}/disk.img,target=/images/disk.img \
            bkahlert/libguestfs@sha256{} \
            --add \
            /images/disk.img \
            --inspector \
            -- \
            <<HERE-{}
            -mkdir-p /home/pi/.ssh 
             -copy-in /shared/home/pi/.ssh/known_hosts /home/pi/.ssh 
            
            !mkdir -p /shared/home/pi/.ssh 
             -copy-out /home/pi/.ssh/known_hosts /shared/home/pi/.ssh 
            
            HERE-{}
        """.trimIndent())
        }
    }

    @Nested
    inner class ForVirtCustomize {
        @Test
        fun `should build proper docker command`() {
            val sshKey = "ssh-rsa ${String.random(20)}== '${String.random(8)}'"

            val osImage: Path = Path.of("/Users/bkahlert/.imgcstmzr.test/guestfish/disk.img")
            val command = VirtCustomizeCommandLine.VirtCustomizeCommandLineBuilder.build {
                options {
                    colors { on }
                    disks { +osImage }
                }

                customizationOptions {
                    sshInject { "pi" to sshKey }
                }
            }

            expectThat(command.adapt().toString()).matchesCurlyPattern("""
            docker \
            run \
            --entrypoint \
            virt-customize \
            --name \
            libguestfs-virt-customize-{} \
            --rm \
            -i \
            --mount \
            type=bind,source={}/shared,target=/shared \
            --mount \
            type=bind,source={}/disk.img,target=/images/disk.img \
            bkahlert/libguestfs@sha256{} \
            --add \
            /images/disk.img \
            --colors \
            --ssh-inject \
            "pi:string:$sshKey"
        """.trimIndent())
        }

        @FiveMinutesTimeout @DockerRequiring @Test
        fun InMemoryLogger.`should set hostname`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            customize(osImage) {
                hostname { "test-machine" }
            }
            expectThat(osImage).mounted(this) {
                path("/etc/hostname") {
                    exists()
                    hasContent("test-machine\n")
                }
            }
        }
    }
}
