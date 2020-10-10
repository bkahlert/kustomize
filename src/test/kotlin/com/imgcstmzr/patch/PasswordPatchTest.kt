package com.imgcstmzr.patch

import com.bkahlert.koodies.string.matches
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.test.strikt.hasMatchingLine
import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.runtime.OperatingSystems
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
import strikt.assertions.containsExactly
import strikt.assertions.isFailure
import java.nio.file.Path
import kotlin.time.ExperimentalTime

@ExperimentalTime
@Execution(ExecutionMode.CONCURRENT)
@ExtendWith(FixtureResolverExtension::class)
internal class PasswordPatchTest {

    val salt = String.random(32)

    @Test
    internal fun `should provide password change command`() {
        val passwordPatch = PasswordPatch("pi", "po", salt)
        val expected = Guestfish.changePasswordCommand("pi", "po", salt)
        expectThat(passwordPatch).matches(guestfishOperationsAssertion = { containsExactly(expected) })
    }

    @DockerRequired
    @Test
    internal fun `should update shadow file correctly`(@OS(RaspberryPiLite::class) img: Path, logger: InMemoryLogger<Unit>) {
        val passwordPath = "/etc/shadow"
        val username = RaspberryPiLite.defaultUsername
        val newPassword = "on-a-diet"
        val passwordPatch = PasswordPatch(username, newPassword, salt)
        val userPassword = Guestfish(img, logger).copyOut(passwordPath).readAllLines().single { it.startsWith(username) }
        val userPasswordPattern = "$username:{}:{}:0:99999:7:::"
        check(userPassword.matches(userPasswordPattern)) { "${userPassword.debug} does not match ${userPasswordPattern.debug}" }

        passwordPatch.patch(img, logger)

        expectThat(img).mounted(logger) {
            path(passwordPath).hasMatchingLine(userPasswordPattern).get { this.single { it.startsWith(username) } }
        }.booted<RaspberryPiLite>(logger) {
            command("echo 'Successfully logged in.'");
            { true }
        }
    }

    @DockerRequired
    @Test
    internal fun `should not be able to use old password`(@OS(RaspberryPiLite::class) img: Path, logger: InMemoryLogger<Unit>) {
        val passwordPatch = PasswordPatch(RaspberryPiLite.defaultUsername, "po", salt)

        passwordPatch.patch(img, logger)

        expectCatching {
            OperatingSystems.credentials[img] = Credentials("pi", "wrong password")
            expectThat(img).booted<RaspberryPiLite>(logger) {
                { output ->
                    if (output.unformatted.contains("Login incorrect", ignoreCase = true)) {
                        kill(); true
                    } else false
                }
            }
        }.isFailure().get { logger.logged.contains("Login incorrect", ignoreCase = true) }
    }
}
