package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * changes the specified [oldUsername] to the specified [newUsername].
 */
class UsernamePatch(
    private val oldUsername: String,
    private val newUsername: String,
) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build("Change Username $oldUsername to $newUsername", osImage) {

        virtCustomize {
            firstBootCommand { "usermod -l $newUsername $oldUsername" }
            @Suppress("SpellCheckingInspection")
            firstBootCommand { "groupmod -n $newUsername $oldUsername" }
            firstBootCommand { "usermod -d ${LinuxRoot.home / newUsername} -m $newUsername" }
        }

        guestfish {
            writeAppendLine(LinuxRoot.etc / "sudoers.d" / "privacy", "Defaults        lecture = never")
            writeAppendLine(LinuxRoot.etc / "sudoers", "$newUsername ALL=(ALL) NOPASSWD:ALL")
        }
    }
}
