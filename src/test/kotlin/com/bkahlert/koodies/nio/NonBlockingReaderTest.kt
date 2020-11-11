package com.bkahlert.koodies.nio

import com.imgcstmzr.runtime.log.RenderingLogger
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.InputStream
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
@Execution(CONCURRENT)
class NonBlockingReaderTest : SharedReaderTest({ inputStream: InputStream, timeout: Duration, logger: RenderingLogger<String?>? ->
    NonBlockingReader(inputStream = inputStream, timeout = timeout, logger = logger, blockOnEmptyLine = false)
})
