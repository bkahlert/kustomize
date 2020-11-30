package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.appendLine
import com.bkahlert.koodies.nio.file.mkdirs
import com.imgcstmzr.patch.files.PasswdDocument
import com.imgcstmzr.patch.new.buildPatch
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.touch
import java.nio.file.Path

class SshAuthorizationPatch(
    username: String,
    private val authorizedKeys: List<String>,
) : Patch by buildPatch("Add ${authorizedKeys.size} authorized SSH keys to $username", {
    fun Path.homePath(entry: PasswdDocument.Entry?) =
        entry?.homeDirectory?.let { home -> asRootFor(Path.of(home)) }

    guestfish {
//        with(".ssh/")
        rootFile("/etc/sudoers.d/privacy", "Defaults        lecture = never")
        @Suppress("SpellCheckingInspection")
        rootFile("/etc/sudoers", "$authorizedKeys ALL=(ALL) NOPASSWD:ALL")
    }

    files {
        var passwdEntry: PasswdDocument.Entry? = null
        edit("/etc/passwd", { requireNotNull(passwdEntry) }) {
            passwdEntry = PasswdDocument(it)[username]
        }
        create({ requireNotNull(it.homePath(passwdEntry)) }) {
            val home = it.homePath(passwdEntry)!!
            val keysFile = home.resolve(".ssh").mkdirs().resolve("authorized_keys").touch()
            authorizedKeys.forEach { key -> keysFile.appendLine(key) }
        }
    }

    postFile {

    }
})
