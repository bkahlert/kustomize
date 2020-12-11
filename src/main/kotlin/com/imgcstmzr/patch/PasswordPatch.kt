package com.imgcstmzr.patch

import com.imgcstmzr.runtime.OperatingSystem

// "Change password according to custom password file?"
class PasswordPatch(
    os: OperatingSystem,
    private val username: String,
    private val password: String,
) : Patch by buildPatch(os, "Change Password of $username", {
    customize { password { com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.PasswordOption.byString(username, password) } }

    postFile {
        updatePassword(username, password)
    }
})
