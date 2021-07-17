package com.imgcstmzr.patch

import com.imgcstmzr.os.OperatingSystemImage
import koodies.unit.BinaryPrefixes
import koodies.unit.Size

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * set increases the available space to [size] by resizing
 * the image itself and subsequently resizing the contained partitions.
 */
class ImgResizePatch(
    private val size: Size,
) : (OperatingSystemImage) -> PhasedPatch {
    override fun invoke(osImage: OperatingSystemImage): PhasedPatch = PhasedPatch.build(
        "Increase Disk Space to ${size.toString(BinaryPrefixes)}",
        osImage,
    ) {
        prepareDisk { resize(size) }

        runPrograms {
            script("finish resize", "sudo raspi-config --expand-rootfs")
        }
    }
}
