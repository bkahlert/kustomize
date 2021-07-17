package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization
import com.imgcstmzr.os.IncorrectPasswordException
import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystem.Credentials
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.os.boot
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import koodies.debug.debug
import koodies.docker.CleanUpMode.ThanksForCleaningUp
import koodies.docker.DockerContainer
import koodies.docker.DockerRequiring
import koodies.exec.Process.State.Excepted
import koodies.exec.rootCause
import koodies.test.CapturedOutput
import koodies.test.FifteenMinutesTimeout
import koodies.test.SystemIOExclusive
import koodies.test.expecting
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.first
import strikt.assertions.hasSize
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.message
import kotlin.io.path.readLines

@SystemIOExclusive
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

    @FifteenMinutesTimeout @DockerRequiring([LibguestfsImage::class, DockerPiImage::class]) @E2E @Test
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
    fun `should not be able to use old password`(container: DockerContainer, @OS(RaspberryPiLite) osImage: OperatingSystemImage, output: CapturedOutput) {
        osImage.patch(PasswordPatch(RaspberryPiLite.defaultCredentials.username, "po"))

        osImage.credentials = Credentials("pi", "wrong password")
        expecting { osImage.boot(container.name) } that {
            isA<Excepted>().rootCause.isA<IncorrectPasswordException>()
                .message.isEqualTo("The entered password \"wrong password\" is incorrect.")
        }
        output.all.contains("Login incorrect")
    }
}
