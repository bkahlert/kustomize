package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.SharedPath

class UsernamePatch(
    oldUsername: String,
    private val newUsername: String,
) : Patch by buildPatch("Change Username $oldUsername to $newUsername", {

    @Suppress("SpellCheckingInspection")
    customizeDisk {
        appendLine {
            val privacyFile = SharedPath.Disk.resolveRoot(it).resolve("/etc/sudoers.d/privacy")
            privacyFile to "Defaults        lecture = never"
        }
        appendLine {
            val sudoersFile = SharedPath.Disk.resolveRoot(it).resolve("/etc/sudoers")
            sudoersFile to "$newUsername ALL=(ALL) NOPASSWD:ALL"
        }
        firstBootCommand { "sudo usermod -l $newUsername $oldUsername" }
        firstBootCommand { "sudo groupmod -n $newUsername $oldUsername" }
        firstBootCommand { "sudo usermod -d /home/$newUsername -m $newUsername" }
    }

    osPrepare {
        updateUsername(oldUsername, newUsername)
    }

    os {
        script("finish rename", "ls /home", "id $oldUsername", "id $newUsername")
    }
})
