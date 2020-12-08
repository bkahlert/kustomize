package com.bkahlert.koodies.nio

import com.imgcstmzr.runtime.log.BlockRenderingLogger
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.InputStream
import kotlin.time.Duration

@Execution(CONCURRENT)
class NonBlockingReaderTest : SharedReaderTest({ inputStream: InputStream, timeout: Duration, logger: BlockRenderingLogger ->
    NonBlockingReader(inputStream = inputStream, timeout = timeout, logger = logger, blockOnEmptyLine = false)
})
