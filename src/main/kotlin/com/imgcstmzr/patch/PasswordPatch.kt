package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption
import com.imgcstmzr.patch.Patch.Companion.buildPatch
import com.imgcstmzr.runtime.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * set the [password] of the user with the specified [username].
 */
class PasswordPatch(
    private val username: String,
    private val password: String,
) : Patch by buildPatch("Change Password of $username", {
    customizeDisk {
        password(VirtCustomizeCustomizationOption.PasswordOption.byString(username, password))
    }

    osPrepare {
        updatePassword(username, password)
    }
})
