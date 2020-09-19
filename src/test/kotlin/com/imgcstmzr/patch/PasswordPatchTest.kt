package com.imgcstmzr.patch

import com.bkahlert.koodies.string.random
import com.imgcstmzr.process.Guestfish
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import strikt.api.expectThat
import strikt.assertions.containsExactly

@Execution(ExecutionMode.CONCURRENT)
@Suppress("RedundantInnerClassModifier")
internal class PasswordPatchTest {

    @Test
    internal fun `should provide password change command`() {
        val salt = String.random(32)
        val patch = PasswordPatch("ingeborg", "finally a secure password", salt)

        val commands = patch.commands

        expectThat(commands).containsExactly(Guestfish.changePasswordCommand("ingeborg", "finally a secure password", salt))
    }
}
