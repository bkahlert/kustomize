package com.imgcstmzr.patch

import com.imgcstmzr.os.LinuxRoot
import com.imgcstmzr.os.OperatingSystemImage

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * changes the specified [oldUsername] to the specified [newUsername].
 */
class UsernamePatch(
    private val oldUsername: String,
    private val newUsername: String,
) : PhasedPatch by PhasedPatch.build("Change Username $oldUsername to $newUsername", {

    @Suppress("SpellCheckingInspection")
    customizeDisk {
        appendLine {
            val privacyFile = LinuxRoot.etc / "sudoers.d" / "privacy"
            "Defaults        lecture = never" to privacyFile
        }
        appendLine {
            val sudoersFile = LinuxRoot.etc / "sudoers"
            "$newUsername ALL=(ALL) NOPASSWD:ALL" to sudoersFile
        }
        firstBootCommand { "usermod -l $newUsername $oldUsername" }
        firstBootCommand { "groupmod -n $newUsername $oldUsername" }
        firstBootCommand { "usermod -d /home/$newUsername -m $newUsername" }
    }

    prepareOs {
        updateUsername(oldUsername, newUsername)
    }

    runPrograms {
        script("finish rename", "ls /home", "id $oldUsername", "id $newUsername")
    }
})
