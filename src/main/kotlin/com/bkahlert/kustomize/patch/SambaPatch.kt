package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.DiskPath
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OperatingSystemImage

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
) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Configure CIFS/SMB/Samba",
        osImage,
    ) {

        val config = StringBuilder().apply {
            appendLine("""
            [global]
            workgroup = smb
            security = user
            map to guest = never
            #unix password sync = yes
            #passwd program = ${LinuxRoot.usr.bin.passwd} %u
            #passwd chat = "*New Password:*" %n\n "*Reenter New Password:*" %n\n "*Password changed.*"
            
        """.trimIndent())

            if (homeShare) {
                appendLine("""
                [home]
                comment = Home of $username
                path = ${LinuxRoot.home / username}
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

        virtCustomize {
            firstBoot {
                command("apt", "-y", "update")
                !"DEBIAN_FRONTEND=noninteractive apt -y -oDpkg::Options::=--force-confnew install samba"
                !"DEBIAN_FRONTEND=noninteractive apt -y -oDpkg::Options::=--force-confnew install uudecode"
                createFile(SAMBA_CONF, config, mode = "0644")
                !"""(echo "$password"; echo "$password") | smbpasswd -s -a "$username""""
            }
        }
    }

    companion object {
        val SAMBA_CONF: DiskPath = LinuxRoot.etc / "samba" / "smb.conf"
    }
}
