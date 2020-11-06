package com.imgcstmzr.patch

import com.bkahlert.koodies.exception.rootCause
import com.bkahlert.koodies.string.matchesCurlyPattern
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.test.strikt.hasMatchingLine
import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.runtime.IncorrectPasswordException
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.Companion.Credentials
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.util.DockerRequired
import com.imgcstmzr.util.FixtureResolverExtension
import com.imgcstmzr.util.OS
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.logging.InMemoryLogger
import com.imgcstmzr.util.matches
import com.imgcstmzr.util.readAllLines
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectCatching
import strikt.api.expectThat
import strikt.assertions.contains
import strikt.assertions.containsExactly
import strikt.assertions.isA
import strikt.assertions.isEqualTo
import strikt.assertions.isFailure
import strikt.assertions.message
import java.util.concurrent.ExecutionException
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(FixtureResolverExtension::class)
class PasswordPatchTest {

    val salt = String.random(32)

    @Test
    fun `should provide password change command`() {
        val passwordPatch = PasswordPatch("pi", "po", salt)
        val expected = Guestfish.changePasswordCommand("pi", "po", salt)
        expectThat(passwordPatch).matches(guestfishOperationsAssertion = { containsExactly(expected) })
    }

    @DockerRequired
    @Test
    fun `should update shadow file correctly`(@OS(RaspberryPiLite::class) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {
        val passwordPath = "/etc/shadow"
        val username = RaspberryPiLite.defaultUsername
        val newPassword = "on-a-diet"
        val passwordPatch = PasswordPatch(username, newPassword, salt)
        val userPassword = Guestfish(osImage, logger).copyOut(passwordPath).readAllLines().single { it.startsWith(username) }
        val userPasswordPattern = "$username:{}:{}:0:99999:7:::"
        check(userPassword.matchesCurlyPattern(userPasswordPattern)) { "${userPassword.debug} does not match ${userPasswordPattern.debug}" }

        passwordPatch.patch(osImage, logger)

        expectThat(osImage.credentials).isEqualTo(Credentials(username, newPassword))
        expectThat(osImage)
            .mounted(logger) {
                path(passwordPath).hasMatchingLine(userPasswordPattern).get { this.single { it.startsWith(username) } }
            }.booted(logger) {
                command("echo '👏 🤓 👋'");
                { true }
            }
    }

    @DockerRequired
    @Test
    fun `should not be able to use old password`(@OS(RaspberryPiLite::class) osImage: OperatingSystemImage, logger: InMemoryLogger<Any>) {
        val passwordPatch = PasswordPatch(RaspberryPiLite.defaultUsername, "po", salt)

        passwordPatch.patch(osImage, logger)

        expectCatching {
            osImage.credentials = Credentials("pi", "wrong password")
            osImage.boot(logger)
        }.isFailure()
            .isA<ExecutionException>()
            .rootCause
            .isA<IncorrectPasswordException>()
            .message.isEqualTo("The entered password \"wrong password\" is incorrect.")
        expectThat(logger.logged).contains("Login incorrect")
    }
}
