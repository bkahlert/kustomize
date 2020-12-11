package com.imgcstmzr.patch

import com.bkahlert.koodies.unit.Size
import com.imgcstmzr.runtime.OperatingSystem

class ImgResizePatch(
    os: OperatingSystem,
    private val size: Size,
) : Patch by buildPatch(os, "Increasing Disk Space: $size", {
    preFile { resize(size) }
})
