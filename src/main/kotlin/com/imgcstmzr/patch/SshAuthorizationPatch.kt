package com.imgcstmzr.patch

import com.imgcstmzr.patch.new.buildPatch

class SshAuthorizationPatch(
    username: String,
    private val authorizedKeys: List<String>,
) : Patch by buildPatch("Add ${authorizedKeys.size} authorized SSH keys to $username", {

    guestfish {
        rootFile("/etc/sudoers.d/privacy", "Defaults        lecture = never")
        @Suppress("SpellCheckingInspection")
        rootFile("/etc/sudoers", "$authorizedKeys ALL=(ALL) NOPASSWD:ALL")
    }

    files {
        @Suppress("SpellCheckingInspection")
        listOf(
            "/etc/passwd", "/etc/group",
            "/etc/shadow", "/etc/gshadow",
            "/etc/subuid", "/etc/subgid",
        ).map { pwFile ->
//            edit(pwFile, Requirements.requireNotMatchingContent(oldUsernamePattern, { "$pwFile must not match $username ($oldUsernamePattern)" })) { path ->
//                path.readAll()
//                    .replace(oldUsernamePattern) { authorizedKeys }
//                    .also { patchedContent -> path.writeText(patchedContent) }
//                    .also { patchedContent -> TermUi.debug("Replaced: ($oldUsernamePattern) -> $newUsername\n$patchedContent") }
//            }
        }
    }

    postFile {
//        updateUsername(authorizedKeys)
    }
})
