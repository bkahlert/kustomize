package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.builder.ListBuilderInit
import com.bkahlert.koodies.builder.buildTo

abstract class CommandLineBuilder {
    companion object {
        fun build(command: String, init: CommandLineBuilder.() -> Unit): CommandLine {
            val redirects = mutableListOf<String>()
            val args = mutableListOf<String>()
            object : CommandLineBuilder() {
                override val redirects
                    get() = redirects
                override val command = command
                override val args
                    get() = args
            }.apply(init)
            return CommandLine(redirects = redirects, command = command, arguments = args.toList())
        }
    }

    protected abstract val redirects: MutableList<String>
    protected abstract val command: String
    protected abstract val args: MutableList<String>

    fun redirects(init: ListBuilderInit<String>) = init.buildTo(redirects)
    fun arguments(init: ListBuilderInit<String>) = init.buildTo(args)
}
