package com.imgcstmzr.patch

import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.HostPath
import com.imgcstmzr.os.OperatingSystemImage
import com.imgcstmzr.patch.Patch.Companion.buildPatch
import koodies.io.path.copyTo
import koodies.io.path.requireExists

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * copies all files and directories (specified by the [hostToDiskMappings]'s [HostPath] instances)
 * into the disk images under each [hostToDiskMappings]'s [DiskPath].
 */
class CopyFilesPatch(private val hostToDiskMappings: Map<HostPath, DiskPath>) :
    Patch by buildPatch("Copy Files: " + hostToDiskMappings.map { (from, to) -> "${from.fileName} ➜ ${to.fileName}" }.joinToString(", "), {

        guestfish {
            hostToDiskMappings.forEach { (hostPath, diskPath) ->
                hostPath.requireExists()
                copyIn {
                    hostPath.copyTo(it.hostPath(diskPath))
                    diskPath
                }
            }
        }

    }) {
    constructor(vararg hostToDiskMappings: Pair<HostPath, DiskPath>) : this(hostToDiskMappings.toMap())
}
