package com.imgcstmzr.runtime;

import com.imgcstmzr.process.Output
import java.nio.file.Path

/**
 * [DietPi](https://dietpi.com)
 */
class DietPi : OS<Workflow> {
    override val downloadUrl: String
        get() = "dietpi.com/downloads/images/DietPi_RPi-ARMv6-Buster.7z"
    override val username = "root"
    override val password = "dietpi"

    override fun increaseDiskSpace(size: Long, img: Path, runtime: Runtime<Workflow>): Int {
        TODO("Not yet implemented")
    }

    override fun bootAndRun(scencario: String, img: Path, runtime: Runtime<Workflow>, processor: RunningOS<Workflow>.(Output) -> Unit): Int {
        TODO("Not yet implemented")
    }

    override fun compileSetupScript(name: String, commandBlocks: String): Array<Workflow> {
        TODO("Not yet implemented")
    }

    override fun compileScript(name: String, vararg commands: String): Workflow {
        TODO("Not yet implemented")
    }
}
