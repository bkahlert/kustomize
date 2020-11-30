package com.imgcstmzr.guestfish

import com.bkahlert.koodies.concurrent.process.IO
import com.imgcstmzr.runtime.log.RenderingLogger

class GuestfishIoProcessor(
    private val renderingLogger: RenderingLogger<*>,
    verbose: Boolean = false,
) : (Any?, IO) -> Unit {

    @Suppress("SpellCheckingInspection")
    private var bootedPattern: Regex = ".*libguestfs: \\[\\d+\\w+] appliance is up.*".toRegex()
    private var logOutput: Boolean = verbose

    var linesSkipped = 0

    override fun invoke(process: Any?, io: IO) {
        if (!logOutput) {
            if (io.unformatted.matches(bootedPattern)) {
                renderingLogger.logLine { IO.Type.META typed "$linesSkipped lines skipped." }
                logOutput = true
            }
            linesSkipped++
        }

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
