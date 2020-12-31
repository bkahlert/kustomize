package com.imgcstmzr.patch

import koodies.unit.Size

class ImgResizePatch(
    private val size: Size,
) : Patch by buildPatch("Increasing Disk Space: $size", {
    prepareDisk { resize(size) }

    os {
        script("finish resize", "expand-root", "sudo raspi-config --expand-rootfs")
    }
})
