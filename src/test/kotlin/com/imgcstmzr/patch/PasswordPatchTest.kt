package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.runGuestfishOn
import com.imgcstmzr.libguestfs.resolveOnDisk
import com.imgcstmzr.libguestfs.resolveOnHost
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.runtime.IncorrectPasswordException
import com.imgcstmzr.runtime.OperatingSystem.Credentials
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.runtime.execute
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.FifteenMinutesTimeout
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.logging.expectThatLogged
import com.imgcstmzr.test.rootCause
import koodies.debug.debug
import koodies.logging.InMemoryLogger
import koodies.text.matchesCurlyPattern
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.first
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.message
import java.util.concurrent.CompletionException
import kotlin.io.path.readLines

@Execution(CONCURRENT)
class PasswordPatchTest {

    @Test
    fun `should provide password change command`(osImage: OperatingSystemImage) {
        val passwordPatch = PasswordPatch(RaspberryPiLite, "pi", "po")
        val expected = VirtCustomizeCustomizationOption.PasswordOption.byString("pi", "po")
        expectThat(passwordPatch).matches(customizationOptionsAssertion = { first().get { invoke(osImage) }.isEqualTo(expected) })
    }

    @FifteenMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should update shadow file correctly`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        val passwordPath = "/etc/shadow"
        val username = RaspberryPiLite.defaultCredentials.username
        val newPassword = "on-a-diet"
        val passwordPatch = PasswordPatch(osImage.operatingSystem, username, newPassword)
        val userPassword = runGuestfishOn(osImage) { copyOut { it.resolveOnDisk(passwordPath) } }
            .let { osImage.resolveOnHost(passwordPath).readLines().single { it.startsWith(username) } }
        val userPasswordPattern = "$username:{}:{}:0:99999:7:::"
        check(userPassword.matchesCurlyPattern(userPasswordPattern)) { "${userPassword.debug} does not match ${userPasswordPattern.debug}" }

        patch(osImage, passwordPatch)

        expectThat(osImage.credentials).isEqualTo(Credentials(username, newPassword))
        expectThat(osImage).booted(this) {
            command("echo '👏 🤓 👋'");
            { true }
        }
    }

    @FifteenMinutesTimeout @E2E @Test
    fun InMemoryLogger.`should not be able to use old password`(@OS(RaspberryPiLite) osImage: OperatingSystemImage) {
        patch(osImage, PasswordPatch(osImage.operatingSystem, RaspberryPiLite.defaultCredentials.username, "po"))

        expectCatching {
            osImage.credentials = Credentials("pi", "wrong password")
            osImage.execute(logger = this, autoLogin = true)
        }.isFailure()
            .isA<CompletionException>()
            .rootCause
            .isA<IncorrectPasswordException>()
            .message.isEqualTo("The entered password \"wrong password\" is incorrect.")
        expectThatLogged().contains("Login incorrect")
    }
}
