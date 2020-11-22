package com.bkahlert.koodies

/**
 * Returns the context ClassLoader for the current [Thread].
 *
 * The context [ClassLoader] is provided by the creator of the [Thread] for use
 * by code running in this thread when loading classes and resources.
 */
val contextClassLoader: ClassLoader get() = Thread.currentThread().contextClassLoader
