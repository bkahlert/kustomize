package com.imgcstmzr.util.logging

import com.imgcstmzr.util.logging.NegativeIndexSupportList.Companion.negativeIndexSupport
import com.imgcstmzr.util.splitLineBreaks

internal class OutputCapture : CapturedOutput {
    companion object {
        fun splitOutput(output: String): List<String> = output.splitLineBreaks().dropLastWhile { it.isBlank() }
    }

    private val systemCaptures: ArrayDeque<SystemCapture> = ArrayDeque()
    fun push() = systemCaptures.addLast(SystemCapture())
    fun pop() = systemCaptures.removeLast().release()

    override val all: String get() = getFilteredCapture { type: Type? -> true }
    override val allLines: List<String> by negativeIndexSupport { splitOutput(all) }

    override val out: String get() = getFilteredCapture { other: Type? -> Type.OUT == other }
    override val outLines: List<String> by negativeIndexSupport { splitOutput(out) }

    override val err: String get() = getFilteredCapture { other: Type? -> Type.ERR == other }
    override val errLines: List<String> by negativeIndexSupport { splitOutput(err) }

    /**
     * Resets the current capture session, clearing its captured output.
     */
    fun reset() = systemCaptures.lastOrNull()?.reset()

    private fun getFilteredCapture(filter: (Type) -> Boolean): String {
        val builder = StringBuilder()
        for (systemCapture in systemCaptures) {
            systemCapture.append(builder, filter)
        }
        return builder.toString()
    }

    /**
     * Types of content that can be captured.
     */
    @Deprecated("Use Output instead")
    enum class Type {
        OUT, ERR
    }


    override fun hashCode(): Int = all.hashCode()

    override fun equals(other: Any?): Boolean {
        if (other === this) {
            return true
        }
        return if (other is CapturedOutput || other is CharSequence) {
            all == other.toString()
        } else false
    }

    override val length: Int get() = all.length
    override fun get(index: Int): Char = all[index]
    override fun subSequence(startIndex: Int, endIndex: Int): CharSequence = all.subSequence(startIndex, endIndex)
    override fun toString(): String = all
}
