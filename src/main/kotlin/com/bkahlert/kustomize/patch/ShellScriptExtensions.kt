package com.bkahlert.kustomize.patch

import com.bkahlert.kommons.builder.buildArray
import com.bkahlert.kommons.builder.buildList
import com.bkahlert.kommons.builder.context.ListBuildingContext
import com.bkahlert.kommons.docker.ubuntu
import com.bkahlert.kommons.exec.CommandLine
import com.bkahlert.kommons.exec.output
import com.bkahlert.kommons.exec.successful
import com.bkahlert.kommons.io.path.fileNameString
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.io.path.tempFile
import com.bkahlert.kommons.io.path.writeBytes
import com.bkahlert.kommons.shell.ShellScript
import com.bkahlert.kommons.shell.ShellScript.ScriptContext
import com.bkahlert.kommons.shell.ShellScript.ScriptContext.Line
import com.bkahlert.kustomize.Kustomize
import com.bkahlert.kustomize.os.DiskPath
import java.nio.file.Path

val ScriptContext.aptGet get() = AptGet(this)
val ScriptContext.apt get() = Apt(this)

/**
 * apt-get is the command-line tool for handling packages, and may be considered
 * the user's "back-end" to other tools using the APT library.
 *
 * Several "front-end" interfaces exist, such as synaptic and aptitude.
 */
class AptGet(private val script: ScriptContext) {

    /**
     * Used to re-synchronize the package index files from their sources.
     */
    fun update(
        allowReleaseInfoChange: Boolean = false,
    ): Line = buildList {
        add("update")
        if (allowReleaseInfoChange) add("--allow-releaseinfo-change")
    }.let { script.command("apt-get", it) }

    /**
     * This option is followed by one or more packages desired for installation.
     */
    fun install(
        /**
         * Each package is a package name, not a fully qualified filename (for instance, in a Fedora Core system, glibc would be the argument provided, not glibc-2.4.8.i686.rpm).
         */
        vararg packages: String,
        /**
         * Automatic yes to prompts. Assume "yes" as answer to all prompts and run non-interactively.
         * If an undesirable situation, such as changing a held package or removing an essential package, occurs then apt-get will abort.
         */
        yes: Boolean = true,
        /**
         * Quiet. Produces output suitable for logging, omitting progress indicators.
         */
        quiet: Int = 2,
        /**
         * Ignore missing packages. If packages cannot be retrieved or fail the integrity check after retrieval (corrupted package files), hold back those packages and handle the result.
         */
        ignoreMissing: Boolean = false,
    ): Line = script.command("apt-get", *arguments(quiet, yes) { if (ignoreMissing) add("-m") }, "install", *packages)

    /**
     * Install shorthand for [install].
     */
    infix fun install(`package`: String): Line = install(packages = arrayOf(`package`))

    /**
     * Uninstalls the optional specified [packages]. Afterwards all no more needed packages will be automatically removed as well.
     */
    fun autoRemove(
        /**
         * Each package is a package name, not a fully qualified filename (for instance, in a Fedora Core system, glibc would be the argument provided, not glibc-2.4.8.i686.rpm).
         */
        vararg packages: String,
        /**
         * Automatic yes to prompts. Assume "yes" as answer to all prompts and run non-interactively.
         * If an undesirable situation, such as changing a held package or removing an essential package, occurs then apt-get will abort.
         */
        yes: Boolean = true,
        /**
         * Quiet. Produces output suitable for logging, omitting progress indicators.
         */
        quiet: Int = 2,
    ): Line = script.command("apt-get", *arguments(quiet, yes), "autoremove", *packages)

    private fun arguments(
        quiet: Int,
        yes: Boolean,
        custom: ListBuildingContext<String>.() -> Unit = {},
    ): Array<out String> =
        buildArray {
            require(quiet in 0..2) { "quiet must be between 0 and 2" }
            if (yes && quiet < 2) add("-y")
            if (quiet == 1) add("-q")
            if (quiet == 2) add("-qq")
            custom()
        }
}

class Apt(private val script: ScriptContext) {
    private fun command(command: String, args: List<String>): Line =
        script.command("apt", command, *args.toTypedArray())

    infix fun list(option: String): Line =
        command("list", listOf(option))
}


// TODO move to kommons and test
@Suppress("unused")
val Kustomize.user: String
    get() = System.getProperty("user.name")
        ?.let { ShellScript("echo $(id -u -r $it):$(id -g -r $it)").exec() }
        ?.takeIf { exec -> exec.successful }?.output
        ?: error("Could not determine user")


fun chown(it: Path, recursive: Boolean = true) {
    it.parent.ubuntu { _ -> command("chown", listOfNotNull("-R".takeIf { recursive }, Kustomize.user, it.fileNameString)) }
}

fun base64(text: String): String =
    base64(text.encodeToByteArray())

fun base64(bytes: ByteArray): String =
    tempFile().writeBytes(bytes).let {
        CommandLine("base64", it.pathString).exec().output
    }

fun ScriptContext.createFile(path: DiskPath, text: String, mode: String? = null) =
    createFile(path, text.encodeToByteArray(), mode)

fun ScriptContext.createFile(path: DiskPath, bytes: ByteArray, mode: String? = null): Line {
    path.parentOrNull.also { !"mkdir -p '$it' &>/dev/null" }
    !"echo '${base64(bytes)}' | base64 --decode > '$path'"
    return mode?.let { command("chmod", it, path.pathString) } ?: command(":")
}
