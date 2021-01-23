package com.imgcstmzr.patch

import koodies.shell.ShellScript

val ShellScript.apt get() = Apt(this)

/**
 * apt-get is the command-line tool for handling packages, and may be considered
 * the user's "back-end" to other tools using the APT library.
 *
 * Several "front-end" interfaces exist, such as synaptic and aptitude.
 */
class Apt(private val shellScript: ShellScript) {
    private fun command(command: String, args: List<String>) {
        shellScript.command("apt-get", *(listOf(command) + args).toTypedArray())
    }

    /**
     * This option is followed by one or more packages desired for installation.
     */
    fun install(
        /**
         * Each package is a package name, not a fully qualified filename (for instance, in a Fedora Core system, glibc would be the argument provided, not glibc-2.4.8.i686.rpm).
         */
        vararg packages: String,
        /**
         * Automatic yes to prompts. Assume "yes" as answer to all prompts and run non-interactively. If an undesirable situation, such as changing a held package or removing an essential package, occurs then apt-get will abort.
         */
        yes: Boolean = true,
        /**
         * Ignore missing packages. If packages cannot be retrieved or fail the integrity check after retrieval (corrupted package files), hold back those packages and handle the result.
         */
        ignoreMissing: Boolean = false,
    ) {
        val args = mutableListOf<String>()
        if (yes) args.add("-y")
        if (ignoreMissing) args.add("-m")
        args.addAll(packages)
        command("install", args)
    }

    /**
     * Install shorthand for [install].
     */
    infix fun install(`package`: String) = install(*arrayOf(`package`))

    /**
     * Uninstalls the optional specified [packages]. Afterwards all no more needed packages will be automatically removed as well.
     */
    fun autoRemove(
        /**
         * Each package is a package name, not a fully qualified filename (for instance, in a Fedora Core system, glibc would be the argument provided, not glibc-2.4.8.i686.rpm).
         */
        vararg packages: String,
        /**
         * Automatic yes to prompts. Assume "yes" as answer to all prompts and run non-interactively. If an undesirable situation, such as changing a held package or removing an essential package, occurs then apt-get will abort.
         */
        yes: Boolean = true,
    ) {
        val args = mutableListOf<String>()
        if (yes) args.add("-y")
        args.addAll(packages)
        command("autoremove", args)
    }

}
