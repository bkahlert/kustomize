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
) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Set Password of $username",
        osImage,
    ) {

        customizeDisk {
            password(PasswordOption.byString(username, password))
        }

        prepareOs {
            updatePassword(username, password)
        }
    }
}
