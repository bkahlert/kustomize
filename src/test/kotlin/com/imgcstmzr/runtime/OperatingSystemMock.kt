package com.imgcstmzr.runtime

import com.bkahlert.koodies.unit.Size
import com.imgcstmzr.process.Output
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import java.nio.file.Path

open class OperatingSystemMock(
    override val name: String = "ImgCstmzr Test OS",
    override val downloadUrl: String? = null,
    override val defaultUsername: String = "",
    override val defaultPassword: String = "",
) : OperatingSystem {
    override fun increaseDiskSpace(
        size: Size,
        img: Path,
        parentLogger: BlockRenderingLogger<Any>?,
    ): Any {
        error(::increaseDiskSpace.name + " not implemented in mock")
    }

    override fun bootToUserSession(
        scenario: String,
        img: Path,
        parentLogger: BlockRenderingLogger<Any>?,
        processor: RunningOS.(Output) -> Any,
    ): Any {
        error(::bootToUserSession.name + " not implemented in mock")
    }

    override fun compileSetupScript(name: String, commandBlocks: String): Array<Program> {
        error(::compileSetupScript.name + " not implemented in mock")
    }

    override fun compileScript(name: String, vararg commands: String): Program {
        error(::compileScript.name + " not implemented in mock")
    }
}
