package com.imgcstmzr.libguestfs

import com.imgcstmzr.libguestfs.guestfish.CopyInCommand
import com.imgcstmzr.libguestfs.guestfish.CopyOutCommand
import com.imgcstmzr.libguestfs.guestfish.ExitCommand
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine
import com.imgcstmzr.libguestfs.guestfish.TarInCommand
import com.imgcstmzr.libguestfs.guestfish.TarOutCommand
import com.imgcstmzr.libguestfs.guestfish.UmountAllCommand
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.isSuccess
import java.nio.file.Path

@Execution(CONCURRENT)
class GuestfishCommandLineTest {

    @Test
    fun `should be instantiatable`() {
        expectCatching { createGuestfishCommand() }.isSuccess()
    }

    @Test
    fun `should break even arguments by default`() {
        expectThat(createGuestfishCommand().toString()).isEqualTo("""
            guestfish \
            --add \
            my/disk.img \
            --inspector \
            !mkdir \
            -p \
            -mkdir-p \
            /home/pi/.ssh \
            -copy-in \
            /shared/home/pi/.ssh/known_hosts \
            /home/pi/.ssh \
            !mkdir \
            -p \
            /shared/home/pi/.ssh \
            copy-out \
            /home/pi/.ssh/known_hosts \
            /shared/home/pi/.ssh \
            tar-in \
            ../archive.tar \
            / \
            tar-out \
            / \
            ../archive.tar \
            umount-all \
            exit
        """.trimIndent())
    }
}

private fun f(path: String): Path = Path.of(path)

internal fun createGuestfishCommand() = GuestfishCommandLine.Companion.build {
    options {
        disks { +Path.of("my/disk.img") }
        inspector { on }
    }

    commands {
        runLocally {
            command("mkdir", "-p")
        }

        ignoreErrors {
            copyIn { CopyInCommand(f("/home/pi/.ssh/known_hosts")) }
        }
        copyOut { CopyOutCommand(f("/home/pi/.ssh/known_hosts")) }

        tarIn { TarInCommand(f("/")) }
        tarOut { TarOutCommand() }
        umountAll { UmountAllCommand() }
        exit { ExitCommand() }
    }
}
