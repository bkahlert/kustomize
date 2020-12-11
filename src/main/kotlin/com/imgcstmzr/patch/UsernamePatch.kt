package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.toPath
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.AppendLineOption
import com.imgcstmzr.runtime.OperatingSystem

class UsernamePatch(
    os: OperatingSystem,
    oldUsername: String,
    private val newUsername: String,
) : Patch by buildPatch(os, "Change Username $oldUsername to $newUsername", {

    @Suppress("SpellCheckingInspection")
    customize {
        appendLine {
            AppendLineOption("/etc/sudoers.d/privacy".toPath(),
                "Defaults        lecture = never")
        }
        appendLine {
            AppendLineOption("/etc/sudoers".toPath(),
                "$newUsername ALL=(ALL) NOPASSWD:ALL")
        }
        firstBootCommand {
            +"sudo usermod -l $newUsername $oldUsername"
            +"sudo groupmod -n $newUsername $oldUsername"
            +"sudo usermod -d /home/$newUsername -m $newUsername"
        }
    }

    postFile {
        updateUsername(newUsername)
    }

    booted {
        script("finish rename", "ls /home", "id $oldUsername", "id $newUsername")
    }
})
