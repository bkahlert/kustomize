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
) : PhasedPatch by PhasedPatch.build("Increase Disk Space to ${size.toString(BinaryPrefixes)}", {
    prepareDisk { resize(size) }

    runPrograms {
        script("finish resize", "sudo raspi-config --expand-rootfs")
    }
})
