package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.builder.ListBuilder
import com.bkahlert.koodies.docker.DockerCommandDsl
import com.bkahlert.koodies.string.LineSeparators
import com.bkahlert.koodies.string.LineSeparators.lines

/**
 * A command as it can be run in a shell.
 */
open class Command(open val redirects: List<String>, val command: String, val args: List<String>) {
    constructor(redirects: List<String> = emptyList(), command: String, vararg args: String) : this(redirects, command, args.toList())

    protected val continuation = " \\${LineSeparators.LF}"
    private val formatted by lazy {
        StringBuilder().also {
            if (redirects.isNotEmpty()) redirects.joinTo(it, separator = " ", postfix = " ")
            it.append(command)
            args.joinTo(it, prefix = continuation, separator = continuation) { arg -> arg }
        }.toString()
    }
    val lines: List<String> by lazy { formatted.lines() }
    override fun toString(): String = formatted
}


@DockerCommandDsl
abstract class CommandBuilder {
    companion object {
        fun build(command: String, init: CommandBuilder.() -> Unit): Command {
            var redirects = emptyList<String>()
            var args = emptyList<String>()
            object : CommandBuilder() {
                override var redirects
                    get() = redirects
                    set(value) = value.run { redirects = this }
                override val command = command
                override var args
                    get() = args
                    set(value) = value.run { args = this }
            }.apply(init)
            return Command(redirects = redirects,
                command = command,
                args = args)
        }
    }

    protected abstract var redirects: List<String>
    protected abstract val command: String
    protected abstract var args: List<String>

    fun redirects(init: ListBuilder<String>.() -> Unit) = ListBuilder.build(init).run { redirects = this }
    fun args(init: ListBuilder<in CharSequence>.() -> Unit) = ListBuilder.build(init) { "$it" }.run { args = this }
}
