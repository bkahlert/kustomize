package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.PasswordOption
import com.bkahlert.kustomize.os.OperatingSystemImage

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

        virtCustomize {
            password(PasswordOption.byString(username, password))
        }
    }
}
