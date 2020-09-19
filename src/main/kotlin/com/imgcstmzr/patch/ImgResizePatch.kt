package com.imgcstmzr.patch

import com.bkahlert.koodies.unit.Size
import com.imgcstmzr.patch.ImgAction.ImgCommandBuilder

class ImgResizePatch(
    private val size: Size,
) : Patch {
    override val name: String = "Resize IMG"

    private val builder = ImgCommandBuilder()

    override val actions: List<Action<*>>
        get() = listOf(ImgAction(builder) {
            resize(size)
        })
}
