package com.imgcstmzr.os.linux

import com.imgcstmzr.os.LinuxRoot
import koodies.shell.ShellScript

data class ServiceScript(
    val name: String,
    val script: ShellScript,
) {
    val diskFile = LinuxRoot.etc.systemd.scripts / name
}
