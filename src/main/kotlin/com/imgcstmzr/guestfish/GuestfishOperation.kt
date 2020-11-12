package com.imgcstmzr.guestfish

import com.bkahlert.koodies.shell.toHereDoc
import com.imgcstmzr.runtime.HasStatus.Companion.asStatus

inline class GuestfishOperation(val commands: Array<String>) {

    constructor(commands: List<String>) : this(commands.toTypedArray())
    constructor(command1: String, command2: String? = null) : this(listOfNotNull(command1, command2).toTypedArray())

    operator fun plus(operation: GuestfishOperation): GuestfishOperation =
        GuestfishOperation(commands + operation.commands)

    operator fun plus(command: String): GuestfishOperation =
        GuestfishOperation(commands + command)

    val summary: String
        get() = commands
            .map { line -> line.split("\\b".toRegex()).filter { x -> x.trim().length > 1 } }
            .map { words ->
                when (words.size) {
                    0 -> "❓"
                    1 -> words.first()
                    2 -> words.joinToString("…")
                    else -> words.first() + "…" + words.last()
                }
            }
            .asStatus()

    val commandCount: Int get() = commands.size

    fun asHereDoc(): String = commands.toHereDoc()
}
