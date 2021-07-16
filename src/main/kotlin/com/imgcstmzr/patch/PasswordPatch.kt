package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.PasswordOption
import com.imgcstmzr.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * set the [password] of the user with the specified [username].
 */
class PasswordPatch(
    private val username: String,
    private val password: String,
) : PhasedPatch by PhasedPatch.build("Set Password of $username", {

    customizeDisk {
        password(PasswordOption.byString(username, password))
    }

    prepareOs {
        updatePassword(username, password)
    }
})
