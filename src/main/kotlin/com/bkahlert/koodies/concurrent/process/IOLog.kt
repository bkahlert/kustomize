package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.IO.Type
import com.bkahlert.koodies.concurrent.synchronized
import com.bkahlert.koodies.string.LineSeparators
import com.bkahlert.koodies.string.LineSeparators.lines
import com.bkahlert.koodies.string.Unicode.Emojis.heavyCheckMark
import org.apache.commons.io.output.ByteArrayOutputStream
import kotlin.collections.Map.Entry

/**
 * An I/O log can be used to log what a [RunningProcess] received and produces as data.
 *
 * In order to log I/O only [add] must be called.
 */
class IOLog {
    /**
     * Contains the currently logged I/O of a [RunningProcess].
     *
     * **Important:** Only complete lines can be accessed as this is considered to be the only safe way
     * to have non-corrupted data (e.g. split characters).
     */
    val logged: List<IO> get() = log

    /**
     * Contains the currently logged I/O. See [logged] for more details.
     */
    private val log = mutableListOf<IO>().synchronized()

    /**
     * Contains not yet fully logged I/O, that is, data not yet terminated by one of the [LineSeparators].
     */
    private val incompleteLines: MutableMap<Type, ByteArrayOutputStream> = mutableMapOf()

    /**
     * Adds [content] with the specified [IO.Type] to the [IOLog].
     *
     * [content] does not have to be *complete* in any way (like a complete line) but also be provided
     * in chunks of any size. The I/O will be correctly reconstructed and can be accessed using [logged].
     */
    fun add(type: Type, content: ByteArray) {
        synchronized(incompleteLines) {
            with(incompleteLines.getOrPut(type, { ByteArrayOutputStream() })) {
                write(content)
                while (true) {
                    val justCompletedLines = incompleteLines.findCompletedLines()
                    if (justCompletedLines != null) log.add(justCompletedLines)
                    else break
                }
            }
        }
    }

    /**
     * Helper to thread-safely
     */
    private fun MutableList<IO>.add(justCompletedLines: Entry<Type, ByteArrayOutputStream>) {
        synchronized(this) {
            val completedLines = justCompletedLines.value.removeCompletedLines()
            log.addAll(justCompletedLines.key typed completedLines)
        }
    }

    private fun Map<Type, ByteArrayOutputStream>.findCompletedLines(): Entry<Type, ByteArrayOutputStream>? =
        entries.firstOrNull { (_, builder) ->
            val toString = builder.toString(Charsets.UTF_8)
            toString.matches(LineSeparators.INTERMEDIARY_LINE_PATTERN)
        }

    private fun ByteArrayOutputStream.removeCompletedLines(): List<String> {
        val read = toString(Charsets.UTF_8).lines()
        return read.take(read.size - 1).also {
            reset()
            write(read.last().toByteArray(Charsets.UTF_8))
        }
    }

    override fun toString(): String =
        "${this::class.simpleName}(${log.size} $heavyCheckMark; ${incompleteLines.filterValues { it.toByteArray().isNotEmpty() }.size} …)"
}

