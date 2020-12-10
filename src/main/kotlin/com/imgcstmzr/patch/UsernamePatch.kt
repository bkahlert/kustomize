package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.readText
import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.nio.file.writeText
import com.imgcstmzr.patch.Requirements.requireNotMatchingContent

// "Change username pi to $username?"
class UsernamePatch(
    oldUsername: String,
    private val newUsername: String,
) : Patch by buildPatch("Change Username $oldUsername to $newUsername", {

    val oldUsernamePattern = Regex("\\b$oldUsername\\b", RegexOption.MULTILINE)

    @Suppress("SpellCheckingInspection")
    customize {
        appendLine {
            com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.AppendLineOption("/etc/sudoers.d/privacy".toPath(),
                "Defaults        lecture = never")
        }
        appendLine {
            com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.AppendLineOption("/etc/sudoers".toPath(),
                "$newUsername ALL=(ALL) NOPASSWD:ALL")
        }
    }

    files {
        @Suppress("SpellCheckingInspection")
        listOf(
            "/etc/passwd", "/etc/group",
            "/etc/shadow", "/etc/gshadow",
            "/etc/subuid", "/etc/subgid",
        ).map { pwFile ->
            edit(pwFile, requireNotMatchingContent(oldUsernamePattern, { "$pwFile must not match $oldUsername ($oldUsernamePattern)" })) { path ->
                path.readText()
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
