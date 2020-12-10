package com.imgcstmzr.libguestfs

import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.test.strikt.matchesCurlyPattern
import com.imgcstmzr.libguestfs.guestfish.CopyInCommand
import com.imgcstmzr.libguestfs.guestfish.CopyOutCommand
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCommandLine
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
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
                        copyIn { CopyInCommand(Path.of("/home/pi/.ssh/known_hosts")) }
                    }
                    copyOut { CopyOutCommand(Path.of("/home/pi/.ssh/known_hosts")) }
                }
            }

            expectThat(command.adapt().toString()).matchesCurlyPattern("""
            docker \
            run \
            --entrypoint \
            guestfish \
            --name \
            libguestfs-guestfish \
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
            copy-out /home/pi/.ssh/known_hosts /shared/home/pi/.ssh
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
            libguestfs-virt-customize \
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
            "pi:string:\"$sshKey\""
        """.trimIndent())
        }
    }
}
