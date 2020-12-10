package com.imgcstmzr.patch

import com.bkahlert.koodies.unit.Size

class ImgResizePatch(
    private val size: Size,
) : Patch by buildPatch("Increasing Disk Space: $size", {
    preFile { resize(size) }
})
