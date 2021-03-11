package com.imgcstmzr.patch

import com.imgcstmzr.patch.Patch.Companion.buildPatch
import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.unit.Size

/**
 * Applied to an [OperatingSystemImage] this [Patch]
 * set increases the available space to [size] by resizing
 * the image itself and subsequently resizing the contained partitions.
 */
class ImgResizePatch(
    private val size: Size,
) : Patch by buildPatch("Increasing Disk Space: $size", {
    prepareDisk { resize(size) }

    os {
        script("finish resize", "sudo raspi-config --expand-rootfs")
    }
})
