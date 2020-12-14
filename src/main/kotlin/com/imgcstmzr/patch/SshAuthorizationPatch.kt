package com.imgcstmzr.patch

import com.imgcstmzr.runtime.OperatingSystem

class SshAuthorizationPatch(
    os: OperatingSystem,
    username: String,
    private val authorizedKeys: List<String>,
) : Patch by buildPatch(os, "Add ${authorizedKeys.size} authorized SSH keys to $username", {

    customize {
        authorizedKeys.forEach { password -> sshInject(username, password) }
    }
})
