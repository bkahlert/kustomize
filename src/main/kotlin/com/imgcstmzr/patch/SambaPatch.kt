package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.patch.Patch.Companion.buildPatch
import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.io.path.withDirectoriesCreated
import kotlin.io.path.writeLines

enum class RootShare { none, `read-only`, `read-write` }

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * installs and configures Samba so that [username] is
 * able to authenticate using the specified [password].
 *
 * Furthermore if [homeShare] is `true` a share with the name `home`
 * pointing is the home directory of user [username] is created.
 *
 * Depending on [rootName] also a share pointing to `/` is created.
 */
class SambaPatch(
    private val username: String,
    private val password: String,
    private val homeShare: Boolean,
    private val rootShare: RootShare,
) : Patch by buildPatch("Configure CIFS/SMB/Samba", {

    val config = StringBuilder().apply {
        appendLine("""
            [global]
            workgroup = smb
            security = user
            map to guest = never
            #unix password sync = yes
            #passwd program = /usr/bin/passwd %u
            #passwd chat = "*New Password:*" %n\n "*Reenter New Password:*" %n\n "*Password changed.*"
            
        """.trimIndent())

        if (homeShare) {
            appendLine("""
                [home]
                comment = Home of $username
                path = /home/$username
                writeable=Yes
                create mask=0744
                directory mask=0744
                public=no
                guest ok=no
                
            """.trimIndent())
        }

        when (rootShare) {
            RootShare.`read-only` -> append("""
                [/]
                comment = Home of $username
                path = /
                writeable=No
                public=no
                guest ok=no
                
            """.trimIndent())

            RootShare.`read-write` -> append("""
                [/]
                path = /
                writeable=Yes
                create mask=0740
                directory mask=0740
                public=no
                guest ok=no
                
            """.trimIndent())

            else -> {
                // no root share
            }
        }
    }.toString()

    customizeDisk {
        firstBootInstall { listOf("samba", "cifs-utils") }
        copyIn(SAMBA_CONF) {
            withDirectoriesCreated().writeLines(config.lines())
        }
        firstBoot("Change SMB Password for $username") {
            !"""
            echo "…"
            echo "…"
            echo "…"
            pass="$password"
            (echo "${'$'}pass"; echo "${'$'}pass") | smbpasswd -s -a "$username"
            """.trimIndent()
        }
        firstBootShutdownCommand()
    }

    boot { yes }

}) {
    companion object {
        val SAMBA_CONF: DiskPath = DiskPath("/etc/samba/smb.conf")
    }
}
