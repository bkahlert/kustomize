package com.imgcstmzr.patch

import com.bkahlert.koodies.string.random
import com.imgcstmzr.patch.new.buildPatch

// "Change password according to custom password file?"
class PasswordPatch(
    private val username: String,
    private val password: String,
    private val salt: String = String.random.cryptSalt(),
) : Patch by buildPatch("Change Password of $username", {
    guestfish { changePassword(username, password, salt) }

    postFile {
        updatePassword(username, password)
    }
})
