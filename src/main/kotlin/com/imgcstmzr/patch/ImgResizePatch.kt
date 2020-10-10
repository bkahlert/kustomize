package com.imgcstmzr.patch

import com.bkahlert.koodies.unit.Size
import com.imgcstmzr.patch.new.Patch
import com.imgcstmzr.patch.new.buildPatch

class ImgResizePatch(
    private val size: Size,
) : Patch by buildPatch("Increasing Disk Space: $size", {
    img { resize(size) }
})
