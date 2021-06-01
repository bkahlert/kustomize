package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.CopyInOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.FirstBootInstallOption
import com.imgcstmzr.libguestfs.VirtCustomizeCommandLine.Customization.FirstBootOption
import com.imgcstmzr.libguestfs.containsFirstBootScriptFix
import com.imgcstmzr.libguestfs.containsFirstBootShutdownCommand
import com.imgcstmzr.libguestfs.file
import com.imgcstmzr.libguestfs.localPath
import com.imgcstmzr.libguestfs.mounted
import com.imgcstmzr.libguestfs.packages
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.os.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.patch.RootShare.`read-write`
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.OS
import koodies.content
import koodies.docker.DockerRequiring
import koodies.logging.InMemoryLogger
import koodies.test.FifteenMinutesTimeout
import koodies.test.Smoke
import koodies.test.UniqueId
import koodies.test.withTempDir
import koodies.text.LineSeparators.lineSequence
import org.junit.jupiter.api.Test
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.filterIsInstance
import strikt.assertions.first
import strikt.assertions.isEqualTo

class SambaPatchTest {

    private val sambaPatch = SambaPatch("the-user", "the-password", true, `read-write`)

    @Test
    fun `should install samba`(osImage: OperatingSystemImage) {
        expectThat(sambaPatch).customizations(osImage) {
            filterIsInstance<FirstBootInstallOption>().first().packages.containsExactlyInAnyOrder("samba", "cifs-utils")
        }
    }

    @Test
    fun `should build samba conf`(osImage: OperatingSystemImage) {
        expectThat(sambaPatch).customizations(osImage) {
            filterIsInstance<CopyInOption>().any {
                localPath.content.isEqualTo(
                    """
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

    @Test
    fun `should set samba password`(osImage: OperatingSystemImage) {
        expectThat(sambaPatch).customizations(osImage) {
            filterIsInstance<FirstBootOption>().any {
                file.content.contains(
                    """
                        echo "…"
                        echo "…"
                        echo "…"
                        pass="the-password"
                        (echo "${'$'}pass"; echo "${'$'}pass") | smbpasswd -s -a "the-user"
                    """.trimIndent())
            }
        }
    }

    @Test
    fun `should shutdown`(osImage: OperatingSystemImage) {
        expectThat(sambaPatch).customizations(osImage) { containsFirstBootShutdownCommand() }
    }

    @Test
    fun `should copy firstboot script order fix`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(sambaPatch).customizations(osImage) { containsFirstBootScriptFix() }
    }

    @FifteenMinutesTimeout @DockerRequiring([LibguestfsImage::class]) @E2E @Smoke @Test
    fun InMemoryLogger.`should install samba and set password and shutdown`(
        uniqueId: UniqueId,
        @OS(RaspberryPiLite) osImage: OperatingSystemImage,
    ) = withTempDir(uniqueId) {

        sambaPatch.patch(osImage)

        expect {
            lateinit var output: Sequence<String>
            that(osImage).booted {
                script { apt list "--installed";"" }.let { output = it };
                { true }
            }
            val packages = output.toList().maxByOrNull { it.length }?.lineSequence()?.filter { it.isNotBlank() }?.toList() ?: emptyList()

            that(packages) {
                any { contains("samba") }
                any { contains("cifs-utils") }
            }

            that(osImage).mounted {
                path("/etc/samba/smb.conf") {
                    content.isEqualTo(
                        """
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
    }
}
