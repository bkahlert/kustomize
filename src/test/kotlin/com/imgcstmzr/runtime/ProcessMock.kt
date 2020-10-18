package com.imgcstmzr.runtime

import com.bkahlert.koodies.string.Grapheme
import com.bkahlert.koodies.terminal.ansi.Style.Companion.magenta
import com.bkahlert.koodies.terminal.ansi.Style.Companion.yellow
import com.bkahlert.koodies.tracing.MiniTracer
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.microTrace
import com.imgcstmzr.runtime.log.miniTrace
import com.imgcstmzr.util.Now
import com.imgcstmzr.util.debug
import org.apache.commons.io.output.TeeOutputStream
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.Duration
import kotlin.time.ExperimentalTime
import kotlin.time.milliseconds
import kotlin.time.seconds

@Suppress("unused")
@OptIn(ExperimentalTime::class)
class ProcessMock(
    private var outputStream: OutputStream = ByteArrayOutputStream(),
    private val inputStream: InputStream = InputStream.nullInputStream(),
    private val processExit: ProcessMock.() -> ProcessExitMock,
    var logger: BlockRenderingLogger<String?>?,
) : Process() {

    private val completeOutputSequence = ByteArrayOutputStream()
    private val unprocessedOutputSequence = outputStream

    init {
        outputStream = TeeOutputStream(completeOutputSequence, unprocessedOutputSequence)
    }

    companion object {
        fun withSlowInput(
            vararg inputs: String,
            baseDelayPerInput: Duration = 1.seconds,
            echoInput: Boolean,
            processExit: ProcessMock.() -> ProcessExitMock,
            logger: BlockRenderingLogger<String?>?,
        ): ProcessMock {
            val outputStream = ByteArrayOutputStream()
            val slowInputStream = SlowInputStream(
                inputs = inputs,
                baseDelayPerInput = baseDelayPerInput,
                byteArrayOutputStream = outputStream,
                echoInput = echoInput,
                logger = logger
            )
            return ProcessMock(
                outputStream = outputStream,
                inputStream = slowInputStream,
                processExit = processExit,
                logger = logger
            )
        }

        fun withIndividuallySlowInput(
            vararg inputs: Pair<Duration, String>,
            baseDelayPerInput: Duration = 1.seconds,
            echoInput: Boolean,
            processExit: ProcessMock.() -> ProcessExitMock,
            logger: BlockRenderingLogger<String?>?,
        ): ProcessMock {
            val outputStream = ByteArrayOutputStream()
            val slowInputStream = SlowInputStream(
                inputs = inputs,
                baseDelayPerInput = baseDelayPerInput,
                byteArrayOutputStream = outputStream,
                echoInput = echoInput,
                logger = logger
            )
            return ProcessMock(
                outputStream = outputStream,
                inputStream = slowInputStream,
                processExit = processExit,
                logger = logger
            )
        }
    }

    override fun getOutputStream(): OutputStream = outputStream
    override fun getInputStream(): InputStream = inputStream
    override fun getErrorStream(): InputStream = InputStream.nullInputStream()
    override fun waitFor(): Int = logger.miniTrace(::waitFor) { this@ProcessMock.processExit()() }
    override fun exitValue(): Int = logger.miniTrace(::exitValue) { this@ProcessMock.processExit()() }
    override fun isAlive(): Boolean {
        return logger.miniTrace(::isAlive) {
            when (inputStream) {
                is SlowInputStream -> inputStream.unreadCount != 0
                else -> processExit().delay > Duration.ZERO
            }
        }
    }

    override fun destroy(): Unit = logger.miniTrace(::destroy) { }

    val received: String get() = completeOutputSequence.toString()

    class SlowInputStream(
        vararg inputs: Pair<Duration, String>,
        val baseDelayPerInput: Duration,
        val byteArrayOutputStream: ByteArrayOutputStream? = null,
        val echoInput: Boolean = false,
        var logger: BlockRenderingLogger<String?>?,
    ) : InputStream() {
        constructor(
            vararg inputs: String,
            baseDelayPerInput: Duration,
            byteArrayOutputStream: ByteArrayOutputStream? = null,
            echoInput: Boolean = false,
            logger: BlockRenderingLogger<String?>?,
        ) : this(inputs = inputs.map { Duration.ZERO to it }.toTypedArray(), baseDelayPerInput, byteArrayOutputStream, echoInput, logger)

        val terminated: Boolean get() = unreadCount == 0 || !processAlive
        var closed = false
        internal var processAlive: Boolean = true
        var blockUntil: Long = System.currentTimeMillis()
        private val unread: MutableList<Pair<Duration, MutableList<Byte>>> = inputs.map { it.first to it.second.toByteArray().toMutableList() }.toMutableList()
        val unreadCount: Int get() = unread.map { it.second.size }.sum()
        private val originalCountLength = unreadCount.toString().length
        private val blockedByPrompt get() = unread.isNotEmpty() && unread.first().first == Duration.INFINITE
        val Int.padded get() = this.toString().padStart(originalCountLength)

        fun processInput(logger: MiniTracer<Boolean>): Boolean = logger.microTrace(Grapheme("✏️")) {
            val input = byteArrayOutputStream?.toString() ?: ""
            if (input.isNotEmpty()) {
                trace(input.debug)
                if (blockedByPrompt) {
                    if (echoInput) unread[0] = Duration.ZERO to input.map { it.toByte() }.toMutableList()
                    else unread.removeFirst()
                    trace("${input.debug} unblocked prompt")
                } else {
                    trace("${input.debug} had no effect, since there was no prompt")
                }
                byteArrayOutputStream?.reset()
            } else {
                if (blockedByPrompt) {
                    trace("blocked by prompt")
                } else {
                    trace("no input and no prompt")
                }
            }
            blockedByPrompt
        }

        fun handleAndReturnBlockingState(): Boolean = logger.miniTrace(::handleAndReturnBlockingState) { processInput(this) }

        companion object {
            fun prompt(): Pair<Duration, String> = Duration.INFINITE to ""
        }

        override fun available(): Int = logger.miniTrace(::available) {
            if (closed) {
                throw IOException("Closed.")
            }

            if (handleAndReturnBlockingState()) {
                trace("prompt is blocking")
                return@miniTrace 0
            }
            val yetBlocked = blockUntil - System.currentTimeMillis()
            if (yetBlocked > 0) {
                trace("${yetBlocked.milliseconds} to wait for next chunk")
                return@miniTrace 0
            }
            if (terminated) {
                trace("Backing buffer is depleted ➜ EOF reached.")
                return@miniTrace 0
            }

            val currentDelayedWord = unread.first()
            if (currentDelayedWord.first > Duration.ZERO) {
                val delay = currentDelayedWord.first
                blockUntil = System.currentTimeMillis() + delay.toLongMilliseconds()
                unread[0] = Duration.ZERO to currentDelayedWord.second
                trace("$delay to wait for next chunk (just started)")
                return@miniTrace 0
            }

            currentDelayedWord.second.size
        }

        override fun read(): Int = logger.miniTrace(::read) {
            if (closed) {
                throw IOException("Closed.")
            }

            while (handleAndReturnBlockingState()) {
                trace("prompt is blocking")
            }

            trace("${unreadCount.padded.yellow()} bytes unread")

            if (terminated) {
                trace("Backing buffer is depleted ➜ EOF reached.")
                return@miniTrace -1
            }

            val yetBlocked = blockUntil - System.currentTimeMillis()
            if (yetBlocked > 0) {
                microTrace<Unit>(Now.grapheme) {
                    trace("blocking for the remaining ${yetBlocked.milliseconds}...")
                    Thread.sleep(yetBlocked)
                }
            }

            val currentWord: MutableList<Byte> = unread.let {
                val currentLine: Pair<Duration, MutableList<Byte>> = it.first()
                val delay = currentLine.first
                if (delay > Duration.ZERO) {
                    this.microTrace<Unit>(Now.grapheme) {
                        trace("output delayed by $delay...")
                        Thread.sleep(delay.toLongMilliseconds())
                        unread[0] = Duration.ZERO to currentLine.second
                    }
                }
                currentLine.second
            }
            trace("— available ${currentWord.debug.magenta()}")
            val currentByte = currentWord.removeFirst()
            trace("— current: $currentByte/${currentByte.toChar()}")

            if (currentWord.isEmpty()) {
                unread.removeFirst()
                blockUntil = System.currentTimeMillis() + baseDelayPerInput.toLongMilliseconds()
                trace("— empty; waiting time for next chunk is $baseDelayPerInput")
            }
            currentByte.toInt()
        }

        /**
         * Tries to behave exactly like [BufferedInputStream.read].
         */
        fun read1(b: ByteArray, off: Int, len: Int): Int {
            val avail = available()
            val cnt = if (avail < len) avail else len
            (0 until cnt).map { i ->
                b[off + i] = read().toByte()
            }
            return cnt
        }

        override fun read(b: ByteArray, off: Int, len: Int): Int {
            if (terminated) {
                return -1
            }

            if (off or len or off + len or b.size - (off + len) < 0) {
                throw IndexOutOfBoundsException()
            } else if (len == 0) {
                return 0
            }

            var n = 0
            while (true) {
                val nread = read1(b, off + n, len - n)
                if (nread <= 0) return if (n == 0) nread else n
                n += nread
                if (n >= len) return n
                // if not closed but no bytes available, return
                if (!closed && available() <= 0) return n
            }
        }

        fun input(text: String): Unit = logger.miniTrace(::input) {
            if (handleAndReturnBlockingState()) {
                trace("Input received: $text")
                unread.removeFirst()
            }
        }

        override fun close() {
            closed = true
        }

        override fun toString(): String = "$unreadCount bytes left"
    }
}

