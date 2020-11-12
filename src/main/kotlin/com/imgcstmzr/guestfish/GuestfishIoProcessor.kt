package com.imgcstmzr.guestfish

import com.bkahlert.koodies.concurrent.process.IO
import com.bkahlert.koodies.docker.DockerProcess
import com.imgcstmzr.runtime.log.RenderingLogger

class GuestfishIoProcessor(private val renderingLogger: RenderingLogger<*>, private val verbose: Boolean = false) :
        (DockerProcess, IO) -> Unit {

    @Suppress("SpellCheckingInspection")
    private var bootedPattern: Regex = ".*libguestfs: \\[\\d+\\w+] appliance is up.*".toRegex()
    private var logOutput: Boolean = verbose

    override fun invoke(process: DockerProcess, io: IO) {
        if (!logOutput && io.unformatted.matches(bootedPattern)) logOutput = true

        if (logOutput) when (io.type) {
            IO.Type.META -> renderingLogger.logLine { io }
            IO.Type.IN -> renderingLogger.logLine { io }
            IO.Type.OUT -> renderingLogger.logLine { io }
            IO.Type.ERR -> throw IllegalStateException(io.unformatted)
        } else {
            if (io.type == IO.Type.ERR) renderingLogger.logLine { io }
        }
    }
}
