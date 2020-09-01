package com.imgcstmzr.runtime

import java.nio.file.Path

/**
 * Capable of running programs using an [OS].
 */
interface Runtime<P : Program<*>> {
    /**
     * Name of this runtime.
     */
    val name: String

    /**
     * Boots the machine using the given [OS] on [img],
     * runs all provided [programs] and finally shuts down the [OS].
     *
     * @return machine's exit code
     */
    fun bootAndRun(scenario: String, os: OS<P>, img: Path, vararg programs: P): Int
}
