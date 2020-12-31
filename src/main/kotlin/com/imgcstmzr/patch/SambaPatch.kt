package com.imgcstmzr.patch

import koodies.io.path.withDirectoriesCreated
import java.nio.file.Path
import kotlin.io.path.writeLines

enum class RootShare { none, `read-only`, `read-write` }

class SambaPatch(
    private val username: String,
    private val password: String,
    private val homeShare: Boolean,
    private val rootShare: RootShare,
) : Patch by buildPatch("Configure CIFS/SMB/Samba", {

    val config = StringBuilder().apply {
        if (homeShare) {
            appendLine("""
                [home]
                path = /home/${username}
                writeable=Yes
                create mask=0744
                directory mask=0744
                public=no
                
            """.trimIndent())
        }

        when (rootShare) {
            RootShare.`read-only` -> append("""
                [/]
                path = /
                writeable=No
                public=no
                
            """.trimIndent())

            RootShare.`read-write` -> append("""
                [/]
                path = /
                writeable=Yes
                create mask=0740
                directory mask=0740
                public=no
                
            """.trimIndent())

            else -> {
                // no root share
            }
        }
    }.toString()

    customizeDisk {
        firstBootInstall { +"samba" }
        copyIn(SAMBA_CONF) {
            withDirectoriesCreated().writeLines(config.lines())
        }
        firstBootCommand { """echo -ne "$password\n$password\n" | smbpasswd -a -s "$username"""" }
    }

}) {
    companion object {
        val SAMBA_CONF: Path = Path.of("/etc/samba/smb.conf")
    }
}
