package com.imgcstmzr.patch

// "Change password according to custom password file?"
class PasswordPatch(
    private val username: String,
    private val password: String,
) : Patch by buildPatch("Change Password of $username", {
    customize { password { com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.PasswordOption.byString(username, password) } }

    postFile {
        updatePassword(username, password)
    }
})
