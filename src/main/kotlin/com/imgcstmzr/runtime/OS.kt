package com.imgcstmzr.runtime

import java.io.File

/**
 * Representation of an operating system that can be customized.
 */
interface OS {
    /**
     * URL that allows an image of this [OS] to be downloaded.
     */
    val downloadUrl: String

    /**
     * [Regex] that matches the [OS]'s output that prompts for the user to login.
     */
    val loginPattern: Regex

    /**
     * [Regex] that matches the [OS]'s output that awaits a command.
     */
    val readyPattern: Regex

    /**
     * Returns the command needed to that this [OS].
     */
    fun startCommand(name: String, img: File): String

    /**
     * Returns [Workflow] that performs a login using [username] and [password].
     */
    fun login(username: String, password: String): Workflow

    /**
     * Returns a [Workflow] that accomplishes [purpose] by executing the given [commandBlocks].
     *
     * The passed command blocks are of the form:
     * ```shell script
     * : no-op using title
     * command
     * command
     * ```
     *
     * Each block is executed en bloc, that is, all commands are passed in a single called—not line-by-line.
     */
    fun sequences(purpose: String, commandBlocks: String): Array<Workflow>

    /**
     * Returns a [Workflow] that accomplishes [purpose] by executing the given [commands].
     *
     * The passed commands are executed line by line.
     */
    fun sequence(purpose: String, vararg commands: String): Workflow

    /**
     * Returns a [Workflow] that accomplishes [purpose] by executing the given [commands] using [sudo](https://en.wikipedia.org/wiki/Sudo)
     *
     * The passed commands are executed line by line.
     */
    fun sudoSequence(purpose: String, vararg commands: String): Workflow

    /**
     * Returns a [Workflow] that initiates the systems immediate shutdown.
     */
    fun shutdown(): Workflow
}

