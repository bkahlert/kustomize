package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.Libguestfs.Companion.hostPath
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.CopyInOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootCommandOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootInstallOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.MkdirOption
import com.imgcstmzr.patch.SambaPatch.Companion.SAMBA_CONF
import com.imgcstmzr.runtime.OperatingSystemImage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.assertions.containsExactly
import strikt.assertions.isEqualTo
import kotlin.io.path.readText

@Execution(CONCURRENT)
class SambaPatchTest {

    @Test
    fun `should provide samba conf copying command`(osImage: OperatingSystemImage) {
        val patch = SambaPatch("the-user", "the-password", true, RootShare.`read-write`)

        expect {
            that(patch).customizations(osImage) {
                containsExactly(listOf(
                    FirstBootInstallOption("samba"),
                    MkdirOption(DiskPath("/etc/samba")),
                    CopyInOption(osImage.hostPath(SAMBA_CONF), DiskPath("/etc/samba")),
                    FirstBootCommandOption("""echo -ne "the-password\nthe-password\n" | smbpasswd -a -s "the-user"""")))
            }
            that(osImage.hostPath(SAMBA_CONF).readText()).isEqualTo("""
                    [global]
                    workgroup = smb
                    security = user
                    map to guest = never
                    #unix password sync = yes
                    #passwd program = /usr/bin/passwd %u
                    #passwd chat = "*New Password:*" %n\n "*Reenter New Password:*" %n\n "*Password changed.*"
                    
                    [home]
                    comment = Home of the-user
                    path = /home/the-user
                    writeable=Yes
                    create mask=0744
                    directory mask=0744
                    public=no
                    guest ok=no
                    
                    [/]
                    path = /
                    writeable=Yes
                    create mask=0740
                    directory mask=0740
                    public=no
                    guest ok=no
                    
                """.trimIndent())
        }
    }
}
