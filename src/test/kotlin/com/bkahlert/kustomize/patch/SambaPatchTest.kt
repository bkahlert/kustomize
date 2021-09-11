package com.bkahlert.kustomize.patch

import com.bkahlert.kommons.io.path.asPath
import com.bkahlert.kommons.io.path.textContent
import com.bkahlert.kommons.junit.UniqueId
import com.bkahlert.kommons.junit.Verbose
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.test.Smoke
import com.bkahlert.kommons.test.withTempDir
import com.bkahlert.kommons.unit.Gibi
import com.bkahlert.kommons.unit.bytes
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.CopyInOption
import com.bkahlert.kustomize.libguestfs.VirtCustomizeCommandLine.VirtCustomization.FirstBootOption
import com.bkahlert.kustomize.libguestfs.containsFirstBootScriptFix
import com.bkahlert.kustomize.libguestfs.file
import com.bkahlert.kustomize.libguestfs.localPath
import com.bkahlert.kustomize.libguestfs.mounted
import com.bkahlert.kustomize.os.LinuxRoot
import com.bkahlert.kustomize.os.OS
import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kustomize.os.OperatingSystems.RaspberryPiLite
import com.bkahlert.kustomize.patch.RootShare.`read-write`
import com.bkahlert.kustomize.test.E2E
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.any
import strikt.assertions.contains
import strikt.assertions.filterIsInstance
import strikt.assertions.isEqualTo
import kotlin.io.path.absolute

class SambaPatchTest {

    private val sambaPatch = SambaPatch("pi", "the-password", true, `read-write`)

    @Test
    fun `should install samba`(osImage: OperatingSystemImage) {
        expectThat(sambaPatch(osImage)).virtCustomizations {
//            filterIsInstance<FirstBootInstallOption>().first().packages.containsExactlyInAnyOrder("samba", "cifs-utils")
            filterIsInstance<FirstBootOption>().any {
                file.textContent {
                    contains("'install'")
                    contains("'samba'")
                }
            }
            filterIsInstance<FirstBootOption>().any {
                file.textContent {
                    contains("'install'")
                    contains("'cifs-utils'")
                }
            }
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

    @BeforeEach @Verbose
    fun xxx() {
        ShellScript("""
            echo "---BEFORE---"
            echo "/home/runner"
            ls /home/runner -alR | grep -w root
            echo "/tmp"
            ls /tmp -alR | grep -w root
        """.trimIndent()).exec.logging()
    }

    @AfterEach @Verbose
    fun yyy() {
        ShellScript("""
            echo "---AFTER---"
            echo "/home/runner"
            ls /home/runner -alR | grep -w root
            echo "/tmp"
            ls /tmp -alR | grep -w root
        """.trimIndent()).exec.logging()
    }

    @E2E @Smoke @Test @Verbose @Disabled("Stops for an unknown reason here: " +
        "    · firstboot.sh[383]: Get:7 http://raspbian.mirror.axinja.net/raspbian buster/main armhf libtevent0 armhf 0.9.37-1 [27.6 kB]\n" +
        "    · firstboot.sh[383]: Get:8 http://raspbian.mirror.axinja.net/raspbian buster/main armhf libldb1 armhf 2:1.5.1+really1.4.6-3+deb10u1 [109 kB]\n" +
        "    · firstboot.sh[383]: Get:9 http://raspbian.mirror.axinja.net/raspbian buster/main armhf libpython2.7 armhf 2.7.16-2+deb10u1 [873 kB]\n" +
        "    · firstboot.sh[383]: Get:10 http://raspbian.mirror.axinja.net/raspbian buster/main armhf python-crypto armhf 2.6.1-9+b1 [248 kB]\n" +
        "    · firstboot.sh[383]: Get:11 http://raspbian.mirror.axinja.net/raspbian buster/main armhf python-gpg armhf 1.12.0-6 [275 kB]\n" +
        "    · firstboot.sh[383]: Get:12 http://raspbian.mirror.axinja.net/raspbian buster/main armhf python-ldb armhf 2:1.5.1+really1.4.6-3+deb10u1 [33.3 kB]\n" +
        "    · firstboot.sh[383]: Get:13 http://raspbian.mirror.axinja.net/raspbian buster/main armhf python-tdb armhf 1.3.16-2+b1 [16.0 kB]\n" +
        "    · firstboot.sh[383]: Get:14 http://raspbian.mirror.axinja.net/raspbian buster/main armhf python-talloc armhf 2.1.14-2 [12.3 kB]\n" +
        "    · firstboot.sh[383]: Get:15 http://raspbian.mirror.axinja.net/raspbian buster/main armhf samba-libs armhf 2:4.9.5+dfsg-5+deb10u1+rpi1 [4,700 kB]\n" +
        "    · firstboot.sh[383]: Get:16 http://raspbian.mirror.axinja.net/raspbian buster/main armhf python-samba armhf 2:4.9.5+dfsg-5+deb10u1+rpi1 [1,794 kB]\n" +
        "    · firstboot.sh[383]: Get:17 http://raspbian.mirror.axinja.net/raspbian buster/main armhf samba-common-bin armhf 2:4.9.5+dfsg-5+deb10u1+rpi1 [570 kB]\n" +
        "    · firstboot.sh[383]: Get:18 http://raspbian.mirror.axinja.net/raspbian buster/main armhf samba-dsdb-modules armhf 2:4.9.5+dfsg-5+deb10u1+rpi1 [345 kB]\n" +
        "    · firstboot.sh[383]: dpkg-preconfigure: unable to re-open stdin: No such file or directory                                                                      \n" +
        "    · firstboot.sh[383]: Fetched 9,817 kB in 11s (917 kB/s)                                                                                                         \n" +
        "    · firstboot.sh[383]: Selecting previously unselected package samba-common.                                                                                      \n" +
        "    · firstboot.sh[383]: (Reading database ...                                                                                                                      \n" +
        "    · (Reading database ... 5%                                                                                                                                      \n" +
        "    · (Reading database ... 10%                                                                                                                                     \n" +
        "    · (Reading database ... 15%                                                                                                                                     \n" +
        "    · (Reading database ... 20%                                                                                                                                     \n" +
        "    · (Reading database ... 25%                                                                                                                                     \n" +
        "    · (Reading database ... 30%                                                                                                                                     \n" +
        "    · (Reading database ... 35%                                                                                                                                     \n" +
        "    · (Reading database ... 40%                                                                                                                                     \n" +
        "    · (Reading database ... 45%                                                                                                                                     \n" +
        "    · (Reading database ... 50%                                                                                                                                     \n" +
        "    · (Reading database ... 55%                                                                                                                                     \n" +
        "    · (Reading database ... 60%                                                                                                                                     \n" +
        "    · (Reading database ... 65%                                                                                                                                     \n" +
        "    · (Reading database ... 70%                                                                                                                                     \n" +
        "    · (Reading database ... 75%                                                                                                                                     \n" +
        "    · (Reading database ... 80%                                                                                                                                     \n" +
        "    · (Reading database ... 85%                                                                                                                                     \n" +
        "    · (Reading database ... 90%                                                                                                                                     \n" +
        "    · (Reading database ... 95%                                                                                                                                     \n" +
        "    · (Reading database ... 100%                                                                                                                                    \n" +
        "    · (Reading database ... 40279 files and directories currently installed.)                                                                                       \n" +
        "    · firstboot.sh[383]: Preparing to unpack .../00-samba-common_2%3a4.9.5+dfsg-5+deb10u1+rpi1_all.deb ...                                                          \n" +
        "    · firstboot.sh[383]: Unpacking samba-common (2:4.9.5+dfsg-5+deb10u1+rpi1) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package libavahi-client3:armhf.\n" +
        "    · firstboot.sh[383]: Preparing to unpack .../01-libavahi-client3_0.7-4+deb10u1_armhf.deb ...                                                                    \n" +
        "    · firstboot.sh[383]: Unpacking libavahi-client3:armhf (0.7-4+deb10u1) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package libcups2:armhf.\n" +
        "    · firstboot.sh[383]: Preparing to unpack .../02-libcups2_2.2.10-6+deb10u4_armhf.deb ...                                                                         \n" +
        "    · firstboot.sh[383]: Unpacking libcups2:armhf (2.2.10-6+deb10u4) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package libgpgme11:armhf.\n" +
        "    · firstboot.sh[383]: Preparing to unpack .../03-libgpgme11_1.12.0-6_armhf.deb ...                                                                               \n" +
        "    · firstboot.sh[383]: Unpacking libgpgme11:armhf (1.12.0-6) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package libjansson4:armhf.\n" +
        "    · firstboot.sh[383]: Preparing to unpack .../04-libjansson4_2.12-1_armhf.deb ...                                                                                \n" +
        "    · firstboot.sh[383]: Unpacking libjansson4:armhf (2.12-1) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package libtdb1:armhf.\n" +
        "    · firstboot.sh[383]: Preparing to unpack .../05-libtdb1_1.3.16-2+b1_armhf.deb ...                                                                               \n" +
        "    · firstboot.sh[383]: Unpacking libtdb1:armhf (1.3.16-2+b1) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package libtevent0:armhf.\n" +
        "    · firstboot.sh[383]: Preparing to unpack .../06-libtevent0_0.9.37-1_armhf.deb ...                                                                               \n" +
        "    · firstboot.sh[383]: Unpacking libtevent0:armhf (0.9.37-1) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package libldb1:armhf.\n" +
        "    · firstboot.sh[383]: Preparing to unpack .../07-libldb1_2%3a1.5.1+really1.4.6-3+deb10u1_armhf.deb ...                                                           \n" +
        "    · firstboot.sh[383]: Unpacking libldb1:armhf (2:1.5.1+really1.4.6-3+deb10u1) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package libpython2.7:armhf.\n" +
        "    · firstboot.sh[383]: Preparing to unpack .../08-libpython2.7_2.7.16-2+deb10u1_armhf.deb ...                                                                     \n" +
        "    · firstboot.sh[383]: Unpacking libpython2.7:armhf (2.7.16-2+deb10u1) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package python-crypto.                                                                                     \n" +
        "    · firstboot.sh[383]: Preparing to unpack .../09-python-crypto_2.6.1-9+b1_armhf.deb ...                                                                          \n" +
        "    · firstboot.sh[383]: Unpacking python-crypto (2.6.1-9+b1) ...                                                                                                   \n" +
        "    · firstboot.sh[383]: Selecting previously unselected package python-gpg.                                                                                        \n" +
        "    · firstboot.sh[383]: Preparing to unpack .../10-python-gpg_1.12.0-6_armhf.deb ...                                                                               \n" +
        "    · firstboot.sh[383]: Unpacking python-gpg (1.12.0-6) ...                                                                                                        \n" +
        "    · firstboot.sh[383]: Selecting previously unselected package python-ldb.                                                                                        \n" +
        "    · firstboot.sh[383]: Preparing to unpack .../11-python-ldb_2%3a1.5.1+really1.4.6-3+deb10u1_armhf.deb ...                                                        \n" +
        "    · firstboot.sh[383]: Unpacking python-ldb (2:1.5.1+really1.4.6-3+deb10u1) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package python-tdb.                                                                                        \n" +
        "    · firstboot.sh[383]: Preparing to unpack .../12-python-tdb_1.3.16-2+b1_armhf.deb ...                                                                            \n" +
        "    · firstboot.sh[383]: Unpacking python-tdb (1.3.16-2+b1) ...                                                                                                     \n" +
        "    · firstboot.sh[383]: Selecting previously unselected package python-talloc:armhf.\n" +
        "    · firstboot.sh[383]: Preparing to unpack .../13-python-talloc_2.1.14-2_armhf.deb ...                                                                            \n" +
        "    · firstboot.sh[383]: Unpacking python-talloc:armhf (2.1.14-2) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package samba-libs:armhf.\n" +
        "    · firstboot.sh[383]: Preparing to unpack .../14-samba-libs_2%3a4.9.5+dfsg-5+deb10u1+rpi1_armhf.deb ...                                                          \n" +
        "    · firstboot.sh[383]: Unpacking samba-libs:armhf (2:4.9.5+dfsg-5+deb10u1+rpi1) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package python-samba.                                                                                      \n" +
        "    · firstboot.sh[383]: Preparing to unpack .../15-python-samba_2%3a4.9.5+dfsg-5+deb10u1+rpi1_armhf.deb ...                                                        \n" +
        "    · firstboot.sh[383]: Unpacking python-samba (2:4.9.5+dfsg-5+deb10u1+rpi1) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package samba-common-bin.                                                                                  \n" +
        "    · firstboot.sh[383]: Preparing to unpack .../16-samba-common-bin_2%3a4.9.5+dfsg-5+deb10u1+rpi1_armhf.deb ...                                                    \n" +
        "    · firstboot.sh[383]: Unpacking samba-common-bin (2:4.9.5+dfsg-5+deb10u1+rpi1) ...\n" +
        "    · firstboot.sh[383]: Selecting previously unselected package samba-dsdb-modules:armhf.\n" +
        "    · firstboot.sh[383]: Preparing to unpack .../17-samba-dsdb-modules_2%3a4.9.5+dfsg-5+deb10u1+rpi1_armhf.deb ...                                                  \n" +
        "    · firstboot.sh[383]: Unpacking samba-dsdb-modules:armhf (2:4.9.5+dfsg-5+deb10u1+rpi1) ...\n" +
        "    · firstboot.sh[383]: Setting up python-crypto (2.6.1-9+b1) ...                                                                                                  \n" +
        "    · firstboot.sh[383]: Setting up libpython2.7:armhf (2.7.16-2+deb10u1) ...\n" +
        "    · firstboot.sh[383]: Setting up libtdb1:armhf (1.3.16-2+b1) ...\n" +
        "    · firstboot.sh[383]: Setting up samba-common (2:4.9.5+dfsg-5+deb10u1+rpi1) ...")
    fun `should install samba and set password and shutdown`(uniqueId: UniqueId, @OS(RaspberryPiLite) osImage: OperatingSystemImage) = withTempDir(uniqueId) {
        val x = osImage.file.absolute().parent

        ShellScript("""
            echo "---OSIMAGE: ${osImage}---"
            echo "/home/runner"
            ls "$x" -alR | grep -w root
        """.trimIndent()).exec.logging()

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
