package com.imgcstmzr.libguestfs

import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.test.junit.FiveMinutesTimeout
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.guestfish.mounted
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine.Companion.virtCustomize
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
            val commandLine = GuestfishCommandLine.build(osImage) {
                env {
                    trace { on }
                    debug { on }
                }

                options {
                    readWrite { on }
                    disk { it.file }
                    inspector { on }
                    mount { Path.of("/dev/sda2") to Path.of("/") }
                    mount { Path.of("/dev/sda1") to Path.of("/boot") }
                }

                commands {
                    runLocally {
                        copyIn { it.resolveOnDocker("/home/pi/.ssh/known_hosts") }
                    }
                    ignoreErrors {
                        copyIn { it.resolveOnDisk("/home/pi/.ssh/known_hosts") }
                    }
                    copyOut { it.resolveOnDisk("/home/pi/.ssh/known_hosts") }
                }
            }

            val dockerRunCommandLine = commandLine.adapt()
            expect {
                that(dockerRunCommandLine.workingDirectory).isEqualTo(osImage.file.parent)
                that(dockerRunCommandLine.toString()).matchesCurlyPattern("""
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
                    type=bind,source=${osImage.file.serialized},target=/images/disk.img \
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
                    
                    HERE-{}
                """.trimIndent())
            }
        }

        @Test
        fun `should log`(osImage: OperatingSystemImage, logger: InMemoryLogger) {
            val commandLine = GuestfishCommandLine.build(osImage) {
                env {
                    trace { on }
                    debug { off }
                }

                options { disk { it.file } }
            }

            commandLine.execute(logger)

            expectThat(logger.logged).contains("libguestfs: trace: set_verbose")
        }
    }

    @Nested
    inner class ForVirtCustomize {

        @Test
        fun `should build proper docker command`(osImage: OperatingSystemImage) {
            val sshKey = "ssh-rsa ${String.random(20)}== '${String.random(8)}'"

            val commandLine = VirtCustomizeCommandLine.build(osImage) {
                options {
                    colors { on }
                    disk { it.file }
                }

                customizationOptions {
                    sshInject("pi", sshKey)
                }
            }

            val dockerRunCommandLine = commandLine.adapt()
            expect {
                that(dockerRunCommandLine.workingDirectory).isEqualTo(osImage.file.parent)
                that(dockerRunCommandLine.toString()).matchesCurlyPattern("""
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
                    type=bind,source=${osImage.file.serialized},target=/images/disk.img \
                    bkahlert/libguestfs@sha256{} \
                    --add \
                    /images/disk.img \
                    --colors \
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