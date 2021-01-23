package com.imgcstmzr.patch

import com.imgcstmzr.libguestfs.DiskPath
import com.imgcstmzr.libguestfs.HostPath
import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.io.path.copyTo
import koodies.io.path.requireExists

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * copies all files and directories (specified by the [hostToDiskMappings]'s [HostPath] instances)
 * into the disk images under each [hostToDiskMappings]'s [DiskPath].
 */
class CopyFilesPatch(private val hostToDiskMappings: Map<HostPath, DiskPath>) :
    Patch by buildPatch(hostToDiskMappings.map { (from, to) -> "${from.fileName} ➜ ${to.fileName}" }.joinToString(", "), {

        customizeDisk {
            hostToDiskMappings.forEach { (hostPath, diskPath) ->
                hostPath.requireExists()
                copyIn(diskPath) { hostPath.copyTo(this) }
            }
        }

    }) {
    constructor(vararg hostToDiskMappings: Pair<HostPath, DiskPath>) : this(hostToDiskMappings.toMap())
}
