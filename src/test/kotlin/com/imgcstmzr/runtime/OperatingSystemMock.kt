package com.imgcstmzr.runtime

import com.bkahlert.koodies.unit.Size
import com.imgcstmzr.process.Output
import java.nio.file.Path

class OperatingSystemMock(
    override val name: String = "ImgCstmzr Test OS",
    override val downloadUrl: String? = null,
    override val username: String = "",
    override val password: String = "",
) : OperatingSystem {
    override fun increaseDiskSpace(size: Size, img: Path, runtime: Runtime): Int {
        TODO("Not yet implemented")
    }

    override fun bootAndRun(scencario: String, img: Path, runtime: Runtime, processor: RunningOS.(Output) -> Unit): Int {
        TODO("Not yet implemented")
    }

    override fun compileSetupScript(name: String, commandBlocks: String): Array<Program> {
        TODO("Not yet implemented")
    }

    override fun compileScript(name: String, vararg commands: String): Program {
        TODO("Not yet implemented")
    }
}
