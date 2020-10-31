package com.bkahlert.koodies.concurrent.process

@ShellScript.ShellScriptMarker
class ShellScript {

    @DslMarker
    annotation class ShellScriptMarker

    val lines: MutableList<String> = mutableListOf()

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
}
