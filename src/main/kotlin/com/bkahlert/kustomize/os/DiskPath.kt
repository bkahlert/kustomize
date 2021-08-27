package com.bkahlert.kustomize.os

import com.bkahlert.kommons.io.path.asPath
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.io.path.resolveBetweenFileSystems
import java.nio.file.Path
import kotlin.io.path.relativeTo

abstract class DiskPath(val pathString: String) {

    val nativePath: Path get() = pathString.asPath()
    val isAbsolute: Boolean get() = nativePath.isAbsolute
    val fileName: Path get() = nativePath.fileName
    val parentOrNull: DiskDirectory? get() = nativePath.parent?.let { DiskDirectory(it.pathString) }
    val parent: DiskDirectory get() = parentOrNull ?: error("Parent required but $pathString has none.")

    /**
     * Returns this path to the host filesystem with the given [mountRoot]
     * as the root.
     */
    fun hostPath(mountRoot: Path): Path {
        val rootRelative = nativePath.takeIf { !it.isAbsolute } ?: nativePath.relativeTo("/".asPath())
        return mountRoot.resolveBetweenFileSystems(rootRelative)
    }

    override fun toString(): String = pathString
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DiskPath

        if (nativePath != other.nativePath) return false

        return true
    }

    override fun hashCode(): Int {
        return nativePath.hashCode()
    }

    companion object {
        val ROOT = LinuxRoot
    }
}

open class DiskDirectory(dir: String) : DiskPath(dir) {
    fun resolve(path: CharSequence): File = File(nativePath.resolve(path.toString()).pathString)
    operator fun div(path: CharSequence): DiskDirectory.File = resolve(path)

    open inner class Directory(dir: String) : DiskDirectory(nativePath.resolve(dir).pathString)
    open inner class File(file: String) : DiskDirectory(nativePath.resolve(file).pathString)
}

@Suppress("ClassName", "SpellCheckingInspection")
object LinuxRoot : DiskDirectory("/") {

    /**
     * **user binaries**: Contains commands such as `cat`, `chmod`, `chgrp`, etc.
     */
    object bin : Directory("bin")

    /**
     * **super user binaries**: Contains commands such as `fsck`, `mount`, `reboot`, etc.
     */
    object sbin : Directory("bin")

    object boot : Directory("boot") {
        object cmdline_txt : File("cmdline.txt")
        object config_txt : File("config.txt")

        /** Marker file to tell Raspberry Pi OS to activate SSH. */
        object ssh : File("ssh")
    }

    object etc : Directory("etc") {

        object apt : Directory("apt") {
            object apt_conf_d : Directory("apt.conf.d") {
                object `80_retries` : File("80-retries")
            }
        }

        object bluetooth : Directory("bluetooth") {
            object main_conf : File("main.conf")
        }

        object crontab : File("crontab")

        object dnsmasq_d : Directory("dnsmasq.d")

        /** DHCP client daemon configuration file */
        object dhcpcd_conf : File("dhcpcd.conf")

        object group : File("group")
        object gshadow : File("gshadow")

        object hostname : File("hostname")

        /** Kernel modules file */
        object modules : File("modules")

        object network : Directory("network") {
            object interfaces_d : File("interfaces.d")
        }

        object passwd : File("passwd")

        object rc_local : File("rc.local")

        object shadow : File("shadow")
        object subgid : File("subgid")
        object subuid : File("subuid")

        object sudoers_d : Directory("sudoers.d") {
            object privacy : File("privacy")
        }

        object systemd : Directory("systemd") {
            object scripts : Directory("scripts")
            object system : Directory("system") {
                object bluetooth_service_d : Directory("bluetooth.service.d")
            }
        }

        object wpa_supplicant : Directory("wpa_supplicant") {
            object wpa_supplicant_conf : File("wpa_supplicant.conf")
        }
    }

    object home : Directory("home")

    object lib : Directory("lib") {
        object systemd : Directory("systemd") {
            object system : Directory("system")
        }
    }

    object root : Directory("root")

    object usr : Directory("usr") {

        /**
         * **OS binaries**: Contains commands such as `dig`, `host`, `sudo`, etc.
         */
        object bin : Directory("bin") {

            /**
             * The passwd command changes passwords for user accounts. A normal
             * user may only change the password for their own account, while
             * the superuser may change the password for any account.  passwd
             * also changes the account or associated password validity period.
             * @see <a href="https://man7.org/linux/man-pages/man1/passwd.1.html">passwd(1) — Linux manual page</a>
             */
            object passwd : File("passwd")
        }

        object lib : Directory("lib")

        object local : Directory("local") {

            /**
             * **user installed binaries**: Contains commands such as `docker`, `fuse-ext2`, `virsh`, etc.
             */
            object bin : Directory("bin")

            /**
             * **super user installed binaries**: Contains commands such as `libvirtd`, `mount_fuse-ext2`, `virtvboxd`, etc.
             */
            object sbin : Directory("sbin")
        }
    }
}
