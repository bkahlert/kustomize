package com.bkahlert.koodies.shell

import com.bkahlert.koodies.concurrent.process.CommandLine
import com.bkahlert.koodies.nio.file.writeText
import com.bkahlert.koodies.string.LineSeparators
import com.bkahlert.koodies.string.LineSeparators.lines
import com.bkahlert.koodies.string.prefixLinesWith
import com.imgcstmzr.util.makeExecutable
import java.nio.file.Path

@DslMarker
annotation class ShellScriptMarker

@ShellScriptMarker
class ShellScript(val name: String? = null, content: String? = null) {

    companion object {
        /**
         * Builds and returns an actual instance.
         */
        fun (ShellScript.() -> Unit).build(): ShellScript =
            ShellScript().apply(this)

        operator fun invoke(name: String? = null, block: ShellScript.() -> Unit): ShellScript {
            val build = block.build()
            val content = build.build()
            return ShellScript(name, content)
        }
    }

    val lines: MutableList<String> = mutableListOf()

    init {
        !`#!`
        if (content != null) lines.addAll(content.trimIndent().lines())
    }

    fun changeDirectoryOrExit(directory: Path, @Suppress("UNUSED_PARAMETER") errorCode: Int = -1) {
        lines.add("cd \"$directory\" || exit -1")
    }

    operator fun String.not() {
        lines.add(this)
    }

    /**
     * Builds a script line based on [words]. All words are combined using a single space.
     */
    fun line(vararg words: String) {
        lines.add(words.joinToString(" "))
    }

    /**
     * Builds a script [line] based on a single string already making up that script.
     */
    fun line(line: String) {
        lines.add(line)
    }

    /**
     * Builds a [command] call using the [arguments].
     */
    fun command(command: String, vararg arguments: String) {
        lines.add(listOf(command, *arguments).joinToString(" "))
    }

    /**
     * Builds a [command] call.
     */
    fun command(command: CommandLine) {
        lines.addAll(command.lines)
    }

    fun exit(code: Int) {
        lines.add("exit $code")
    }

    fun comment(text: String) {
        lines += text.prefixLinesWith(prefix = "# ")
    }

    fun build(): String = lines.joinToString(LineSeparators.LF, postfix = LineSeparators.LF)
    fun buildTo(path: Path): Path = path.apply {
        writeText(build())
        makeExecutable()
    }

    override fun toString(): String = "Script(name=$name;content=${build().lines(ignoreTrailingSeparator = true).joinToString(";")}})"
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ShellScript

        if (name != other.name) return false
        if (lines != other.lines) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name?.hashCode() ?: 0
        result = 31 * result + lines.hashCode()
        return result
    }
}
