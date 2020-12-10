package com.imgcstmzr.patch

import com.bkahlert.koodies.exception.rootCause
import com.bkahlert.koodies.nio.file.readLines
import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.string.matchesCurlyPattern
import com.bkahlert.koodies.test.junit.FifteenMinutesTimeout
import com.imgcstmzr.E2E
import com.imgcstmzr.libguestfs.guestfish.CopyOutCommand
import com.imgcstmzr.libguestfs.guestfish.GuestfishCommandLine.Companion.fish
import com.imgcstmzr.libguestfs.resolveOnHost
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.runtime.IncorrectPasswordException
import com.imgcstmzr.runtime.OperatingSystem.Credentials
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.runtime.execute
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.matches
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

@Execution(CONCURRENT)
class PasswordPatchTest {

    @Test
    fun `should provide password change command`() {
        val passwordPatch = PasswordPatch("pi", "po")
        val expected = VirtCustomizeCustomizationOption.PasswordOption.byString("pi", "po")
        expectThat(passwordPatch).matches(customizationOptionsAssertion = { first().isEqualTo(expected) })
    }

    @FifteenMinutesTimeout @E2E @Test
    fun `should update shadow file correctly`(@OS(RaspberryPiLite) osImage: OperatingSystemImage, logger: InMemoryLogger) {
        val passwordPath = "/etc/shadow"
        val username = RaspberryPiLite.defaultCredentials.username
        val newPassword = "on-a-diet"
        val passwordPatch = PasswordPatch(username, newPassword)
        val userPassword = logger.fish(osImage) { copyOut { CopyOutCommand(passwordPath.toPath()) } }
            .let { osImage.resolveOnHost(passwordPath).readLines().single { it.startsWith(username) } }
//        Guestfish(osImage, logger).copyOut(passwordPath).readLines().single { it.startsWith(username) }
        val userPasswordPattern = "$username:{}:{}:0:99999:7:::"
        check(userPassword.matchesCurlyPattern(userPasswordPattern)) { "${userPassword.debug} does not match ${userPasswordPattern.debug}" }

        passwordPatch.patch(osImage, logger)

        expectThat(osImage.credentials).isEqualTo(Credentials(username, newPassword))
        expectThat(osImage).booted(logger) {
            command("echo '👏 🤓 👋'");
            { true }
        }
    }

    @FifteenMinutesTimeout @E2E @Test
    fun `should not be able to use old password`(@OS(RaspberryPiLite) osImage: OperatingSystemImage, logger: InMemoryLogger) {
        val passwordPatch = PasswordPatch(RaspberryPiLite.defaultCredentials.username, "po")

        passwordPatch.patch(osImage, logger)

        expectCatching {
            osImage.credentials = Credentials("pi", "wrong password")
            osImage.execute(logger = logger, autoLogin = true)
        }.isFailure()
            .isA<CompletionException>()
            .rootCause
            .isA<IncorrectPasswordException>()
            .message.isEqualTo("The entered password \"wrong password\" is incorrect.")
        expectThat(logger.logged).contains("Login incorrect")
    }
}
