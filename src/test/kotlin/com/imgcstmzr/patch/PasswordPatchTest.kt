package com.imgcstmzr.patch

import com.imgcstmzr.expectRendered
import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization
import com.imgcstmzr.os.IncorrectPasswordException
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OS
import com.imgcstmzr.os.OperatingSystem.Credentials
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.os.boot
import com.imgcstmzr.test.E2E
import koodies.debug.debug
import koodies.docker.CleanUpMode.ThanksForCleaningUp
import koodies.docker.DockerContainer
import koodies.docker.DockerRequiring
import koodies.exec.Process.State.Excepted
import koodies.exec.rootCause
import koodies.test.FifteenMinutesTimeout
import koodies.test.expecting
import koodies.text.ansiRemoved
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.message
import kotlin.io.path.readLines

class PasswordPatchTest {

    @Test
    fun `should provide password change command`(osImage: OperatingSystemImage) {
        val passwordPatch = PasswordPatch("pi", "po")
        val expected = Customization.PasswordOption.byString("pi", "po")
        expectThat(passwordPatch(osImage)).matches(
            diskCustomizationsAssertion = { first().isEqualTo(expected) },
            osPreparationsAssertion = { hasSize(1) }
        )
    }

    @E2E @Test
    fun `should update shadow file correctly`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val passwordPath = LinuxRoot.etc.shadow
        val username = RaspberryPiLite.defaultCredentials.username
        val newPassword = "on-a-diet"
        val passwordPatch = PasswordPatch(username, newPassword)
        val userPassword = osImage.guestfish {
            copyOut { passwordPath }
        }.let { osImage.hostPath(passwordPath).readLines().single { it.startsWith(username) } }
        val userPasswordPattern = "$username:{}:{}:0:99999:7:::"
        check(userPassword.matchesCurlyPattern(userPasswordPattern)) { "${userPassword.debug} does not match ${userPasswordPattern.debug}" }

        osImage.patch(passwordPatch)

        expectThat(osImage.credentials).isEqualTo(Credentials(username, newPassword))
        expectThat(osImage).booted {
            command("echo '👏 🤓 👋'");
            { true }
        }
    }

    @FifteenMinutesTimeout @DockerRequiring([LibguestfsImage::class, DockerPiImage::class], ThanksForCleaningUp) @E2E @Test
    fun `should not be able to use old password`(container: DockerContainer, @OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        osImage.patch(PasswordPatch(RaspberryPiLite.defaultCredentials.username, "po"))

        osImage.credentials = Credentials("pi", "wrong password")
        expecting { osImage.boot(container.name) } that {
            isA<Excepted>().rootCause.isA<IncorrectPasswordException>()
                .message.isEqualTo("The entered password \"wrong password\" is incorrect.")
        }
        expectRendered().ansiRemoved {
            contains("Login incorrect")
        }
    }
}
