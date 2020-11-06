package com.imgcstmzr.patch

import com.imgcstmzr.patch.Requirements.requireNotMatchingContent
import com.imgcstmzr.patch.new.Patch
import com.imgcstmzr.patch.new.buildPatch
import com.imgcstmzr.util.readAll
import com.imgcstmzr.util.writeText

// "Change username pi to $username?"
class UsernamePatch(
    oldUsername: String,
    private val newUsername: String,
) : Patch by buildPatch("Change Username $oldUsername to $newUsername", {

    val oldUsernamePattern = Regex("\\b$oldUsername\\b", RegexOption.MULTILINE)

    guestfish {
        rootFile("/etc/sudoers.d/privacy", "Defaults        lecture = never")
        @Suppress("SpellCheckingInspection")
        rootFile("/etc/sudoers", "$newUsername ALL=(ALL) NOPASSWD:ALL")
    }

    files {
        @Suppress("SpellCheckingInspection")
        listOf(
            "/etc/passwd", "/etc/group",
            "/etc/shadow", "/etc/gshadow",
            "/etc/subuid", "/etc/subgid",
        ).map { pwFile ->
            edit(pwFile, requireNotMatchingContent(oldUsernamePattern, { "$pwFile must not match $oldUsername ($oldUsernamePattern)" })) { path ->
                path.readAll()
                    .replace(oldUsernamePattern) { newUsername }
                    .also { patchedContent -> path.writeText(patchedContent) }
//                    .also { patchedContent -> TermUi.debug("Replaced: ($oldUsernamePattern) -> $newUsername\n$patchedContent") }
            }
        }
    }

    postFile {
        updateUsername(newUsername)
    }
})
