package com.imgcstmzr.patch

import com.imgcstmzr.patch.new.Patch
import com.imgcstmzr.patch.new.SimplePatch

class CompositePatch(
    private val patches: Collection<Patch>,
) : Patch by SimplePatch(
    patches.joinToString(" + ") { it.name },
    patches.flatMap { it.preFileImgOperations }.toList(),
    patches.flatMap { it.guestfishOperations }.toList(),
    patches.flatMap { it.fileSystemOperations }.toList(),
    patches.flatMap { it.postFileImgOperations }.toList(),
    patches.flatMap { it.programs }.toList(),
)
