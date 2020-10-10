package com.imgcstmzr.util.logging

interface InMemoryLoggerFactory<T> {
    fun createLogger(customSuffix: String, borderedOutput: Boolean = true): InMemoryLogger<T>
}
