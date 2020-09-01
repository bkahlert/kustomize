package com.imgcstmzr.runtime

import com.github.ajalt.clikt.output.TermUi.echo
import com.github.ajalt.mordant.TermColors
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.time.Duration

class FakeProcess(
    val output: OutputStream = ByteArrayOutputStream(),
    val input: InputStream = InputStream.nullInputStream(),
    var exitValue: Int? = null,
) :
    Process() {
    override fun getOutputStream(): OutputStream = output
    override fun getInputStream(): InputStream = input
    override fun getErrorStream(): InputStream = InputStream.nullInputStream()
    override fun waitFor(): Int = 0
    override fun exitValue(): Int = exitValue ?: throw IllegalStateException("Process not terminated yet!")
    override fun destroy() {}
    override fun isAlive(): Boolean = if (input is SlowInputStream) input.unreadCount != 0 else exitValue == null

    fun exit(exitValue: Int) {
        if (input is SlowInputStream) input.terminated = true
        this.exitValue = exitValue
    }

    class SlowInputStream(vararg inputs: String, val delay: Duration) : InputStream() {
        var terminated = false
        var pretendEmptyTill = System.currentTimeMillis()
        val unread: MutableList<Pair<MutableList<Byte>?, Duration?>> = inputs.map {
            kotlin.runCatching { null to Duration.parse(it) }.getOrDefault(it.toByteArray().toMutableList() to null)
        }.toMutableList()
        val unreadCount: Int get() = unread.map { it.first?.size ?: 0 }.sum()

        override fun available(): Int {
            if (pretendEmptyTill - System.currentTimeMillis() > 0) return 0

            if (unread.firstOrNull()?.second != null) {
                val duration = unread.firstOrNull()?.second!!
                echo(TermColors().gray("Sleeping for $duration"))
                pretendEmptyTill = System.currentTimeMillis() + duration.toMillis()
                unread.removeFirst()
                return 0
            }

            return if (unread.isEmpty()) 0 else unread.first().first?.size ?: 0
        }

        override fun read(): Int {

            echo(termColors.gray("$this"))
            (pretendEmptyTill - System.currentTimeMillis()).takeIf { it > 0 }?.also { Thread.sleep(it) }

            if (terminated || unread.isEmpty()) {
                return -1
            }

            val currentWord: MutableList<Byte> =
                unread.first().first
                    ?: unread.also { // penalty if reading too soon (first element has no bytes anymore)
                        Thread.sleep(500)
                        it.removeFirst()
                    }.first().first!!
            val currentByte = currentWord.removeFirst()
            if (currentWord.isEmpty()) {
                unread.removeFirst()
                pretendEmptyTill = System.currentTimeMillis() + delay.toMillis()
            }
            return currentByte.toInt()
        }

        override fun toString(): String = "$unreadCount bytes left"
    }

    companion object {
        fun withSlowInput(vararg input: String, delay: Duration = Duration.ofSeconds(1)): FakeProcess =
            FakeProcess(input = SlowInputStream(*input, delay = delay))
    }
}

val termColors = TermColors()
