package com.imgcstmzr.util.logging

import com.bkahlert.koodies.collections.withNegativeIndices
import com.bkahlert.koodies.concurrent.synchronized
import com.bkahlert.koodies.string.LineSeparators.withoutTrailingLineSeparator
import com.bkahlert.koodies.string.TruncationStrategy.MIDDLE
import com.bkahlert.koodies.string.padStartFixedLength
import com.bkahlert.koodies.string.prefixLinesWith
import com.bkahlert.koodies.terminal.ansi.AnsiCode.Companion.removeEscapeSequences
import com.bkahlert.koodies.time.Now
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import org.apache.commons.io.output.TeeOutputStream
import java.io.OutputStream

open class InMemoryLogger private constructor(
    caption: CharSequence,
    borderedOutput: Boolean = false,
    statusInformationColumn: Int = -1,
    private val outputStream: TeeOutputStream,
    private val captured: MutableList<String>,
    private val start: Long,
) : BlockRenderingLogger(
    caption = caption,
    borderedOutput = borderedOutput,
    statusInformationColumn = if (statusInformationColumn > 0) statusInformationColumn else 60,
    log = { message: String ->
        val thread = Thread.currentThread().name.padStartFixedLength(30, strategy = MIDDLE)
        val time = Now.passedSince(start).toString().padStartFixedLength(7)
        val prefix = "$thread: $time: "
        outputStream.write(message.prefixLinesWith(prefix = prefix).toByteArray())
        captured.add(message.withoutTrailingLineSeparator)
    },
) {
    constructor(
        caption: String,
        borderedOutput: Boolean = true,
        statusInformationColumn: Int = -1,
        outputStreams: List<OutputStream>,
    ) : this(
        caption = caption,
        borderedOutput = borderedOutput,
        statusInformationColumn = statusInformationColumn,
        outputStream = outputStreams.foldRight(TeeOutputStream(OutputStream.nullOutputStream(), OutputStream.nullOutputStream()),
            { os, tos -> TeeOutputStream(os, tos) }),
        captured = mutableListOf<String>().synchronized(),
        start = System.currentTimeMillis(),
    )

    constructor() : this("Test", true, -1, emptyList())

    private val messages: List<CharSequence> by withNegativeIndices { captured }
    private val raw: String get() = messages.joinToString("\n")
    val logged: String get() = messages.joinToString("\n").removeEscapeSequences().withoutTrailingLineSeparator.trim()

    /**
     * Returns the so far rendered content—pretending this block was finished with [result].
     */
    fun <R> finalizedDump(result: Result<R>): String = raw + "\n" + getBlockEnd(result)

    @Suppress("UNCHECKED_CAST")
    override fun toString(): String = finalizedDump(Result.success(Unit) as Result<Nothing>)
}
