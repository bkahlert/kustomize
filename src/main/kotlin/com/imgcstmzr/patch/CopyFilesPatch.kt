package com.imgcstmzr.patch

import com.imgcstmzr.os.DiskPath
import com.imgcstmzr.os.OperatingSystemImage
import koodies.io.path.copyTo
import koodies.io.path.isSubPathOf
import koodies.io.path.requireExists
import java.nio.file.Path

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * copies all files and directories (specified by the [hostToDiskMappings]'s [HostPath] instances)
 * into the disk images under each [hostToDiskMappings]'s [DiskPath].
 */
class CopyFilesPatch(
    private val hostToDiskMappings: Map<() -> Path, DiskPath>,
) : (OperatingSystemImage) -> PhasedPatch {

    constructor(vararg hostToDiskMappings: Pair<() -> Path, DiskPath>) : this(hostToDiskMappings.toMap())

    override fun invoke(osImage: OperatingSystemImage): PhasedPatch {
        val files = hostToDiskMappings.map { (from, to) -> from() to to }
        return PhasedPatch.build(
            "Copy Files: " + files.map { (from, to) -> "${from.fileName} ➜ ${to.fileName}" }.joinToString(", "),
            osImage,
        ) {
            modifyDisk {
                files.forEach { (path, diskPath) ->
                    path.requireExists()
                    copyIn {
                        require(!path.isSubPathOf(it.exchangeDirectory)) { "$path must be located outside of ${it.exchangeDirectory}" }
                        val hostPath = it.hostPath(diskPath)
                        path.copyTo(hostPath)
                        diskPath
                    }
                }
            }
        }
    }
}
