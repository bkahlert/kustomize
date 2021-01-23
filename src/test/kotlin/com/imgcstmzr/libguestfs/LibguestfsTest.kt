package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.Libguestfs.Companion.libguestfs
import com.imgcstmzr.libguestfs.guestfish.mounted
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.Companion.virtCustomize
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.DockerRequiring
import com.imgcstmzr.test.FiveMinutesTimeout
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.hasContent
import com.imgcstmzr.test.logging.expectThatLogged
import com.imgcstmzr.test.matchesCurlyPattern
import koodies.io.path.asString
import koodies.logging.InMemoryLogger
import koodies.text.randomString
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.exists
import strikt.assertions.isEqualTo
import java.nio.file.Path

@Execution(CONCURRENT)
class LibguestfsTest {

    @Nested
    inner class ForGuestfish {

        @Test
        fun `should build proper docker command`(osImage: OperatingSystemImage) {
            val commandLine = osImage.libguestfs().guestfish {
                env {
                    trace { on }
                    debug { on }
                }

                options {
                    readWrite { on }
                    disk { it.file }
                    inspector { on }
                    mount { Path.of("/dev/sda2") to DiskPath("/") }
                    mount { Path.of("/dev/sda1") to DiskPath("/boot") }
                }

                commands {
                    runLocally {
                        copyIn { DiskPath("/home/pi/.ssh/known_hosts") }
                    }
                    ignoreErrors {
                        copyIn { DiskPath("/home/pi/.ssh/known_hosts") }
                    }
                    copyOut { DiskPath("/home/pi/.ssh/known_hosts") }
                }
            }.dockerCommandLine()

            expect {
                that(commandLine.workingDirectory).isEqualTo(osImage.file.parent)
                that(commandLine.toString()).matchesCurlyPattern("""
                    docker \
                    run \
                    --env \
                    LIBGUESTFS_TRACE=1 \
                    --env \
                    LIBGUESTFS_DEBUG=1 \
                    --entrypoint \
                    guestfish \
                    --name \
                    libguestfs-guestfish-{} \
                    --rm \
                    -i \
                    --mount \
                    type=bind,source=${osImage.directory.resolve("shared")},target=/shared \
                    --mount \
                    type=bind,source=${osImage.file.asString()},target=/images/disk.img \
                    bkahlert/libguestfs@sha256{} \
                    --add \
                    /images/disk.img \
                    --rw \
                    --inspector \
                    --mount \
                    /dev/sda2:/ \
                    --mount \
                    /dev/sda1:/boot \
                    -- \
                    <<HERE-{}
                    !-mkdir-p /home/pi/.ssh 
                     -copy-in /shared/home/pi/.ssh/known_hosts /home/pi/.ssh 
                    
                    -mkdir-p /home/pi/.ssh 
                     -copy-in /shared/home/pi/.ssh/known_hosts /home/pi/.ssh 
                    
                    !mkdir -p /shared/home/pi/.ssh 
                     -copy-out /home/pi/.ssh/known_hosts /shared/home/pi/.ssh 
                    
                    umount-all
                    exit
                    HERE-{}
                """.trimIndent())
            }
        }

        @Test
        fun InMemoryLogger.`should log`(osImage: OperatingSystemImage) {
            val commandLine = osImage.libguestfs().guestfish {
                env {
                    trace { on }
                    debug { off }
                }

                options { disk { it.file } }
            }

            with(commandLine) { executeLogging() }

            expectThatLogged().contains("libguestfs: trace: set_verbose")
        }
    }

    @Nested
    inner class ForVirtCustomize {

        @Test
        fun `should build proper docker command`(osImage: OperatingSystemImage) {
            val sshKey = "ssh-rsa ${randomString(20)}== '${randomString(8)}'"

            val commandLine = osImage.libguestfs().virtCustomize {
                options {
                    colors { on }
                    disk { it.file }
                }

                customizationOptions {
                    sshInject("pi", sshKey)
                }
            }.dockerCommandLine()

            expect {
                that(commandLine.workingDirectory).isEqualTo(osImage.file.parent)
                that(commandLine.toString()).matchesCurlyPattern("""
                    docker \
                    run \
                    --entrypoint \
                    virt-customize \
                    --name \
                    libguestfs-virt-customize-{} \
                    --rm \
                    -i \
                    --mount \
                    type=bind,source=${osImage.directory.resolve("shared")},target=/shared \
                    --mount \
                    type=bind,source=${osImage.file.asString()},target=/images/disk.img \
                    bkahlert/libguestfs@sha256{} \
                    --colors \
                    --add \
                    /images/disk.img \
                    --ssh-inject \
                    "pi:string:$sshKey"
                """.trimIndent())
            }
        }

        @FiveMinutesTimeout @DockerRequiring @Test
        fun InMemoryLogger.`should set hostname`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
            virtCustomize(osImage) {
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
