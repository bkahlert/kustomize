package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.DiskPath
import com.bkahlert.kustomize.os.OperatingSystemImage
import koodies.io.isInside
import koodies.io.path.copyTo
import koodies.io.path.requireExists
import java.nio.file.Path

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * copies all files and directories as specified by [hostToDiskMappings].
 */
class CopyFilesPatch(
    private val hostToDiskMappings: Map<() -> Path, DiskPath>,
) : (OperatingSystemImage) -> PhasedPatch {

    constructor(vararg hostToDiskMappings: Pair<() -> Path, DiskPath>) : this(hostToDiskMappings.toMap())

    override fun invoke(osImage: OperatingSystemImage): PhasedPatch {
        val files = hostToDiskMappings.map { (from, to) -> from() to to }
        return PhasedPatch.build(
            "Copy Files: " + files.joinToString { (from, to) -> "${from.fileName} ➜ ${to.fileName}" },
            osImage,
        ) {
            guestfish {
                files.forEach { (path, diskPath) ->
                    path.requireExists()
                    copyIn {
                        require(!path.isInside(it.exchangeDirectory)) { "$path must be located outside of ${it.exchangeDirectory}" }
                        val hostPath = it.hostPath(diskPath)
                        path.copyTo(hostPath)
                        diskPath
                    }
                }
            }
        }
    }
}
