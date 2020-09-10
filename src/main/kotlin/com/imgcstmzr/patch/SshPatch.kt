package com.imgcstmzr.patch

import com.imgcstmzr.util.exists
import com.imgcstmzr.util.touch
import java.nio.file.Path

class SshPatch : Patch {
    override val name: String = "Enabled SSH"
    override val actions: List<PathAction>
        get() = listOf(
            PathAction(Path.of("/boot/ssh"), {
                require(it.exists) { "$it is missing" }
            })
            { path ->
                path.touch()
            },
        )
}
