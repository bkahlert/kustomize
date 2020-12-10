package com.imgcstmzr.patch

class SshAuthorizationPatch(
    username: String,
    private val authorizedKeys: List<String>,
) : Patch by buildPatch("Add ${authorizedKeys.size} authorized SSH keys to $username", {

    customize {
        authorizedKeys.forEach { sshInject { username to it } }
    }
})
