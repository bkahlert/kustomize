package com.imgcstmzr.process

import java.io.InputStream
import java.nio.ByteBuffer
import java.time.Duration

class NonBlockingReader(
    private val isAlive: () -> Boolean,
    private val inputStream: InputStream,
    timeout: Duration = Duration.ofMillis(100),
    capacity: Int = 8 * 1024 * 1024,
) {
    constructor(
        process: Process,
        stream: Process.() -> InputStream,
        timeout: Duration = Duration.ofMillis(100),
        capacity: Int = 8 * 1024 * 1024,
    ) : this({ process.isAlive }, process.stream(), timeout, capacity)

    private val buffer = ByteBuffer.wrap(ByteArray(capacity))
    private val timeoutMillis = timeout.toMillis()
    private val lineFeed = '\r'
    private val carriageReturn = '\n'
    private val lineBreak = "$lineFeed?$carriageReturn".toRegex()


    fun readLine(): String? {
        var maxTimeMillis = System.currentTimeMillis() + timeoutMillis
        buffer.clear()
        while (buffer.position() < buffer.limit()) {
            if (System.currentTimeMillis() >= maxTimeMillis) {
                return String(buffer.array(), 0, buffer.position()).also { buffer.clear() }
            }
            val readLength = inputStream.available().coerceAtMost(buffer.remaining())
            val readResult = inputStream.read(buffer.array(), buffer.position(), readLength)
            when (readResult) {
                -1 -> return null
                0 -> {
                    if (!isAlive()) {
                        return null
                    }
                    continue
                }
            }
            buffer.position(buffer.position() + readResult)

            val read = String(buffer.array(), 0, buffer.position())
            val split = read.split(lineBreak, 2)
            if (split.size == 2) {
                val lineBreakLength = read.length - (split[1].length + split[0].length)
                buffer.position(split[0].length + lineBreakLength)
                return split[0]
            }

            // read something but no line break encountered -> reset timer
            maxTimeMillis = System.currentTimeMillis() + timeoutMillis
        }
        throw IllegalStateException("A line was read which exceeds the limit of ${buffer.capacity()}")
    }

    fun forEachLine(block: (String) -> Unit) {
        while (true) {
            val line = readLine() ?: break
            block.invoke(line)
        }
    }
}
