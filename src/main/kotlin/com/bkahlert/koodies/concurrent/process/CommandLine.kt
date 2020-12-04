package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.concurrent.process.Processes.isTempScriptFile
import com.bkahlert.koodies.nio.file.Paths
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.nio.file.toPath
import com.bkahlert.koodies.shell.ShellScript
import com.bkahlert.koodies.shell.shebang
import com.bkahlert.koodies.string.LineSeparators
import com.bkahlert.koodies.string.unquoted
import com.imgcstmzr.util.get
import org.codehaus.plexus.util.StringUtils
import org.codehaus.plexus.util.cli.CommandLineUtils
import java.nio.file.Path
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.MULTILINE
import org.codehaus.plexus.util.cli.Commandline as PlexusCommandLine

/**
 * A command as it can be run in a shell.
 */
open class CommandLine(
    val redirects: List<String>,
    val command: String,
    val arguments: List<String>,
) {
    constructor(
        command: String,
        vararg arguments: String,
    ) : this(emptyList(), command, arguments.toList())

    private val formattedRedirects = redirects.takeIf { it.isNotEmpty() }?.joinToString(separator = " ", postfix = " ") ?: ""

    /**
     * The array consisting of the command and its arguments that make up this command,
     * e.g. `[echo, Hello World!]`.
     */
    val commandLineParts: Array<String> by lazy { arrayOf(command, *arguments.toTypedArray()) }

    /**
     * The command line as it can be used on the shell,
     * e.g. `echo "Hello World!"`.
     */
    val commandLine: String by lazy { CommandLineUtils.toString(commandLineParts).fixHereDoc() }

    /**
     * The command line as it can be used on the shell, but in contrast to [commandLine],
     * this version eventually spans multiple lines using escaped line separators to be
     * easier readable, e.g.
     * ```shell
     * command \
     * --argument \
     * "argument" \
     * -org
     * ```
     */
    val multiLineCommandLine: String by lazy {
        commandLineParts.joinToString(separator = " \\${LineSeparators.LF}") { StringUtils.quoteAndEscape(it.trim(), '\"') }.fixHereDoc()
    }

    companion object {
        val hereDocStartRegex: Regex = Regex("<<(?<name>\\w[-\\w]*)\\s*")
        fun String.fixHereDoc(): String {
            var fixed = this
            val hereDocNames = hereDocStartRegex.findAll(fixed).map { it["name"] }
            hereDocNames.forEach { name ->
                fixed = Regex("[\"']+<<$name.*^$name[\"']+", setOf(MULTILINE, DOT_MATCHES_ALL)).replace(fixed) {
                    var hereDoc = it.value
                    while (true) {
                        val unquoted = hereDoc.unquoted
                        if (unquoted != hereDoc) hereDoc = unquoted
                        else break
                    }
                    hereDoc
                }
            }

            return fixed
        }
    }


    /**
     * The array consisting of the single quoted command and its arguments,
     * e.g. `[/bin/sh, -c, 'echo' 'Hello World!']`
     */
    private val bashLineParts: Array<String> by lazy { PlexusCommandLine(commandLine).shellCommandline }

    /**
     * Single quoted command line,
     * e.g. `'echo' 'Hello World!'`
     */
    val singleQuotedCommandLine: String get() = bashLineParts.last()

    /**
     * The command line as it can be used on the shell,
     * e.g. `/bin/sh -c "'echo' 'Hello World!'"`.
     */
    val bashLine: String get() = CommandLineUtils.toString(bashLineParts)

    override fun toString(): String = multiLineCommandLine

    val lines: List<String> get() = toString().lines()

    /**
     * Builds a proper script that runs at [workingDirectory] and saves it as a
     * temporary file (to be deleted once in a while).
     */
    fun toScript(workingDirectory: Path?): Path =
        ShellScript().apply {
            shebang
            workingDirectory?.also { changeDirectoryOrExit(directory = it) }
            command(commandLine)
        }.buildTo(Processes.tempScriptFile())

    fun execute(workingDirectory: Path = Paths.WorkingDirectory, environment: Map<String, String>): Process {
        val scriptFile = if (command.toPath().isTempScriptFile()) commandLine else toScript(workingDirectory).serialized
        return PlexusCommandLine("/bin/sh -c $scriptFile").run {
            addArguments(arguments)
            @Suppress("ExplicitThis")
            this.workingDirectory = workingDirectory.toFile()
            environment.forEach { addEnvironment(it.key, it.value) }
            execute()
        }
    }
}
