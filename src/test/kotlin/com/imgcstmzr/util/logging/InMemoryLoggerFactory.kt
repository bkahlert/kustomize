package com.imgcstmzr.util.logging

interface InMemoryLoggerFactory {
    fun createLogger(customSuffix: String, borderedOutput: Boolean = true): InMemoryLogger
}
