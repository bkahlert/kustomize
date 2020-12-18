package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.runtime.OperatingSystem

// "Change password according to custom password file?"
class PasswordPatch(
    os: OperatingSystem,
    private val username: String,
    private val password: String,
) : Patch by buildPatch("Change Password of $username", {
    customizeDisk {
        password { VirtCustomizeCustomizationOption.PasswordOption.byString(username, password) }
    }

    osPrepare {
        updatePassword(username, password)
    }
})
