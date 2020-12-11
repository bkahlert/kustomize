package com.bkahlert.koodies.nio

import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import java.io.InputStream
import kotlin.time.Duration

@Execution(CONCURRENT)
class NonBlockingReaderTest : SharedReaderTest({ inputStream: InputStream, timeout: Duration ->
    NonBlockingReader(inputStream = inputStream, timeout = timeout, logger = this, blockOnEmptyLine = false)
})
