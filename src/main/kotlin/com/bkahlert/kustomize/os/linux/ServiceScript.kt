package com.bkahlert.kustomize.os.linux

import com.bkahlert.kustomize.os.LinuxRoot
import koodies.shell.ShellScript

data class ServiceScript(
    val name: String,
    val script: ShellScript,
) {
    val diskFile = LinuxRoot.etc.systemd.scripts / name
}
