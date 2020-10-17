package com.imgcstmzr.runtime

import com.bkahlert.koodies.terminal.ansi.Style.Companion.magenta
import com.bkahlert.koodies.terminal.ansi.Style.Companion.yellow
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.miniTrace
import com.imgcstmzr.util.Now
import com.imgcstmzr.util.debug
import org.apache.commons.io.output.TeeOutputStream
import java.io.ByteArrayOutputStream
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
        internal var processAlive: Boolean = true
        var blockUntil: Long = System.currentTimeMillis()
        private val unread: MutableList<Pair<Duration, MutableList<Byte>>> = inputs.map { it.first to it.second.toByteArray().toMutableList() }.toMutableList()
        val unreadCount: Int get() = unread.map { it.second.size }.sum()
        private val originalCountLength = unreadCount.toString().length
        val Int.padded get() = this.toString().padStart(originalCountLength)
        fun processInput(): Unit = logger.miniTrace(::processInput) {
            val input = byteArrayOutputStream?.toString() ?: ""
            if (input.isNotEmpty()) {
                trace(input.debug)
                if (unread.isNotEmpty() && unread.first().first == Duration.INFINITE) {
                    if (echoInput) unread[0] = Duration.ZERO to input.map { it.toByte() }.toMutableList()
                    else unread.removeFirst()
                    trace("unblocked")
                }
                byteArrayOutputStream?.reset()
            }
        }

        fun handleAndReturnBlockingState(): Boolean = logger.miniTrace(::handleAndReturnBlockingState) {
            processInput()
            return@miniTrace unread.isNotEmpty() && unread.first().first == Duration.INFINITE
        }

        companion object {
            fun prompt(): Pair<Duration, String> = Duration.INFINITE to ""
        }

        override fun available(): Int = logger.miniTrace(::available) {
            if (handleAndReturnBlockingState()) {
                trace("prompt is blocking")
                return@miniTrace 0
            }
            val yetBlocked = blockUntil - System.currentTimeMillis()
            if (yetBlocked > 0) {
                trace("${yetBlocked.milliseconds} to wait for next chunk")
                return@miniTrace 0
            }
            if (unread.isEmpty()) {
                trace("Backing buffer is depleted.")
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
            while (handleAndReturnBlockingState()) {
                trace("prompt is blocking")
            }

            trace("${unreadCount.padded.yellow()} bytes unread")

            if (terminated) {
                trace("EOF reached.")
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

        fun input(text: String): Unit = logger.miniTrace(::input) {
            if (handleAndReturnBlockingState()) {
                trace("Input received: $text")
                unread.removeFirst()
            }
        }

        override fun toString(): String = "$unreadCount bytes left"
    }
}

