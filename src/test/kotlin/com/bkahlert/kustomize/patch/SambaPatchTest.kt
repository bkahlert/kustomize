package com.bkahlert.kustomize.patch

import com.bkahlert.kommons.io.path.asPath
import com.bkahlert.kommons.io.path.textContent
import com.bkahlert.kommons.junit.UniqueId
import com.bkahlert.kommons.test.Smoke
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.unit.Gibi
import com.bkahlert.kommons.unit.bytes
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.CopyInOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.FirstBootInstallOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.FirstBootOption
import com.bkahlert.kustomize.libguestfs.containsFirstBootScriptFix
import com.bkahlert.kustomize.libguestfs.file
import com.bkahlert.kustomize.libguestfs.localPath
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.libguestfs.packages
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import com.bkahlert.kustomize.patch.RootShare.`read-write`
import com.bkahlert.kustomize.test.E2E
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.containsExactlyInAnyOrder
import strikt.assertions.filterIsInstance
import strikt.assertions.first
import strikt.assertions.isEqualTo

class SambaPatchTest {

    private val sambaPatch = SambaPatch("pi", "the-password", true, `read-write`)

    @Test
    fun `should install samba`(osImage: OperatingSystemImage) {
        expectThat(sambaPatch(osImage)).virtCustomizations {
            filterIsInstance<FirstBootInstallOption>().first().packages.containsExactlyInAnyOrder("samba", "cifs-utils")
        }
    }

    @Test
    fun `should build samba conf`(osImage: OperatingSystemImage) {
        expectThat(sambaPatch(osImage)).virtCustomizations {
            filterIsInstance<CopyInOption>().any {
                localPath.textContent.isEqualTo(
                    """
                    [global]
                    workgroup = smb
                    security = user
                    map to guest = never
                    #unix password sync = yes
                    #passwd program = ${LinuxRoot.usr.bin.passwd} %u
                    #passwd chat = "*New Password:*" %n\n "*Reenter New Password:*" %n\n "*Password changed.*"
                    
                    [home]
                    comment = Home of pi
                    path = ${LinuxRoot.home / "pi"}
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
        expectThat(sambaPatch(osImage)).virtCustomizations {
            filterIsInstance<FirstBootOption>().any {
                file.textContent.contains(
                    """
                        echo "…"
                        echo "…"
                        echo "…"
                        pass="the-password"
                        (echo "${'$'}pass"; echo "${'$'}pass") | smbpasswd -s -a "pi"
                    """.trimIndent())
            }
        }
    }

    @Test
    fun `should copy firstboot script order fix`(osImage: OperatingSystemImage, uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(sambaPatch(osImage)).virtCustomizations { containsFirstBootScriptFix() }
    }

    @E2E @Smoke @Test
    fun `should install samba and set password and shutdown`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) = withTempDir(uniqueId) {

        osImage.patch(CompositePatch(ResizePatch(2.Gibi.bytes), sambaPatch))

        val installedPackages = "/root/installed.txt"
        osImage.patch {
            virtCustomize {
                firstBoot {
                    apt list "--installed" redirectTo installedPackages.asPath()
                    shutdown
                }
            }
            boot()
        }

        expectThat(osImage).mounted {
            path(installedPackages) {
                textContent {
                    contains("samba")
                    contains("cifs-utils")
                }
            }
            path("/etc/samba/smb.conf") {
                textContent.isEqualTo("""
                    [global]
                    workgroup = smb
                    security = user
                    map to guest = never
                    #unix password sync = yes
                    #passwd program = ${LinuxRoot.usr.bin.passwd} %u
                    #passwd chat = "*New Password:*" %n\n "*Reenter New Password:*" %n\n "*Password changed.*"
    
                    [home]
                    comment = Home of pi
                    path = ${LinuxRoot.home / "pi"}
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
