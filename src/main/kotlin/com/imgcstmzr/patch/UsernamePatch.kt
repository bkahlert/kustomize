package com.imgcstmzr.patch

import com.github.ajalt.clikt.output.TermUi
import com.imgcstmzr.patch.Requirements.requireNotMatchingContent
import com.imgcstmzr.patch.new.Patch
import com.imgcstmzr.patch.new.buildPatch
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.readAll
import com.imgcstmzr.util.writeText

// "Change username pi to $username?"
class UsernamePatch(
    oldUsername: String,
    private val newUsername: String,
) : Patch by buildPatch("Change Username $oldUsername to $newUsername", {

    val oldUsernamePattern = Regex("\\b$oldUsername\\b", RegexOption.MULTILINE)

    files {
        @Suppress("SpellCheckingInspection")
        listOf(
            "/etc/passwd", "/etc/group",
            "/etc/shadow", "/etc/gshadow",
            "/etc/subuid", "/etc/subgid",
        ).map {
            edit(it, requireNotMatchingContent(oldUsernamePattern, { "$it must not match $oldUsername ($oldUsernamePattern)" })) { path ->
                path.readAll()
                    .replace(oldUsernamePattern) { newUsername }
                    .also { patchedContent -> path.writeText(patchedContent) }
                    .also { patchedContent -> TermUi.debug("Replaced: ($oldUsernamePattern) -> $newUsername\n$patchedContent") }
            }
        }
    }
})
