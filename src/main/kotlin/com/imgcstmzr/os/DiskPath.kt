package com.imgcstmzr.os

import koodies.io.file.resolveBetweenFileSystems
import koodies.io.path.asPath
import koodies.io.path.pathString
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

object LinuxRoot : DiskDirectory("/") {

    object boot : Directory("boot") {
        object CmdlineTxt : File("cmdline.txt")
        object ConfigTxt : File("config.txt")
    }

    object etc : Directory("etc") {
        object systemd : Directory("systemd") {
            object scripts : Directory("scripts")
            object system : Directory("system")
        }

        object passwd : File("passwd")
        object group : File("group")
        object shadow : File("shadow")
        object gshadow : File("gshadow")
        object subuid : File("subuid")
        object subgid : File("subgid")
    }

    object lib : Directory("lib") {
        object systemd : Directory("systemd") {
            object system : Directory("system")
        }
    }

    object home : Directory("home")
    object root : Directory("root")
    object usr : Directory("usr")
}
