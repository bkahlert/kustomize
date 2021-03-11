package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.guestfish.mounted
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.CopyInOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootInstallOption
import com.imgcstmzr.libguestfs.virtcustomize.VirtCustomizeCustomizationOption.FirstBootOption
import com.imgcstmzr.libguestfs.virtcustomize.containsFirstBootScriptFix
import com.imgcstmzr.libguestfs.virtcustomize.containsFirstBootShutdownCommand
import com.imgcstmzr.libguestfs.virtcustomize.file
import com.imgcstmzr.libguestfs.virtcustomize.localPath
import com.imgcstmzr.libguestfs.virtcustomize.packages
import com.imgcstmzr.patch.RootShare.`read-write`
import com.imgcstmzr.runtime.OperatingSystemImage
import com.imgcstmzr.runtime.OperatingSystems.RaspberryPiLite
import com.imgcstmzr.test.E2E
import com.imgcstmzr.test.FifteenMinutesTimeout
import com.imgcstmzr.test.OS
import com.imgcstmzr.test.Smoke
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.test.content
import com.imgcstmzr.withTempDir
import koodies.logging.InMemoryLogger
import koodies.text.LineSeparators.lineSequence
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expect
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.filterIsInstance
import strikt.assertions.first
import strikt.assertions.isEqualTo

@Execution(CONCURRENT)
class SambaPatchTest {

    private val patch = SambaPatch("the-user", "the-password", true, `read-write`)

    @Test
    fun `should install samba`(osImage: OperatingSystemImage) {
        expectThat(patch).customizations(osImage) {
            filterIsInstance<FirstBootInstallOption>().first().packages.containsExactlyInAnyOrder("samba", "cifs-utils")
        }
    }

    @Test
    fun `should build samba conf`(osImage: OperatingSystemImage) {
        expectThat(patch).customizations(osImage) {
            filterIsInstance<CopyInOption>().first().localPath.content.isEqualTo(
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

    @Test
    fun `should set samba password`(osImage: OperatingSystemImage) {
        expectThat(patch).customizations(osImage) {
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
        expectThat(patch).customizations(osImage) { containsFirstBootShutdownCommand() }
    }

    @Test
    fun `should copy firstboot script order fix`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(patch).customizations(osImage) { containsFirstBootScriptFix() }
    }

    // TODO    @DockerRequiring(["bkahlert/libguestfs"])
    @FifteenMinutesTimeout @E2E @Smoke @Test
    fun `should run install samba and set password and shutdown`(
        logger: InMemoryLogger,
        uniqueId: UniqueId,
        @OS(RaspberryPiLite) osImage: OperatingSystemImage,
    ) = withTempDir(uniqueId) {

        patch.patch(osImage, logger)

        expect {
            lateinit var output: Sequence<String>
            that(osImage).booted(logger) {
                script { apt list "--installed" }.let { output = it };
                { true }
            }
            val packages = output.toList().maxByOrNull { it.length }?.lineSequence()?.filter { it.isNotBlank() }?.toList() ?: emptyList()

            that(packages) {
                any { contains("samba") }
                any { contains("cifs-utils") }
            }

            that(osImage).mounted(logger) {
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
