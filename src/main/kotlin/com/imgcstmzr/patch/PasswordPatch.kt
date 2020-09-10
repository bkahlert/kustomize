package com.imgcstmzr.patch

import com.imgcstmzr.patch.GuestfishAction.GuestfishCommandBuilder
import com.imgcstmzr.util.random

// "Change password according to custom password file?"
class PasswordPatch(
    private val username: String,
    private val password: String,
    private val salt: String = String.random(32),
) : Patch {

    override val name = "Password Change"

    private val builder = GuestfishCommandBuilder()

    override val actions: List<Action<*>>
        get() = listOf(GuestfishAction(builder) {
            changePassword(username, password, salt)
        })
}
