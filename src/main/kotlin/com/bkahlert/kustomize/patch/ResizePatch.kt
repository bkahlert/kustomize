package com.bkahlert.kustomize.patch

import com.bkahlert.kustomize.os.OperatingSystemImage
import com.bkahlert.kommons.unit.BinaryPrefixes
import com.bkahlert.kommons.unit.Size

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * set increases the available space to [size] by resizing
 * the image itself and subsequently resizing the contained partitions.
 */
class ResizePatch(
    private val size: Size,
) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Increase Disk Space to ${size.toString(BinaryPrefixes)}",
        osImage,
    ) {
        disk { resize(size) }
    }
}
