package com.bkahlert.koodies.concurrent.process

import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.regex.get
import com.bkahlert.koodies.shell.ShellScript
import com.bkahlert.koodies.shell.shebang
import com.bkahlert.koodies.string.LineSeparators
import com.bkahlert.koodies.string.unquoted
import com.bkahlert.koodies.terminal.ANSI
import com.github.ajalt.mordant.AnsiColorCode
import com.imgcstmzr.runtime.HasStatus.Companion.asStatus
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.runtime.log.logging
import org.codehaus.plexus.util.StringUtils
import org.codehaus.plexus.util.cli.CommandLineUtils
import java.io.InputStream
import java.nio.file.Path
import kotlin.text.RegexOption.DOT_MATCHES_ALL
import kotlin.text.RegexOption.MULTILINE

/**
 * A command as it can be run in a shell.
 */
open class CommandLine(
    val redirects: List<String>,
    val environment: Map<String, String>,
    open val workingDirectory: Path,
    val command: String,
    val arguments: List<String>,
) {
    constructor(
        environment: Map<String, String>,
        workingDirectory: Path,
        command: String,
        vararg arguments: String,
    ) : this(emptyList(), environment, workingDirectory, command, arguments.toList())

    constructor(
        environment: Map<String, String>,
        workingDirectory: Path,
        command: Path,
        vararg arguments: String,
    ) : this(environment, workingDirectory, command.serialized, *arguments)

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
    val commandLine: String by lazy {
        formattedRedirects + CommandLineUtils.toString(commandLineParts).fixHereDoc()
    }

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

    val summary: String
        get() = arguments
            .map { line -> line.split("\\b".toRegex()).filter { part -> part.trim().run { length > 1 && !startsWith("-") } } }
            .filter { it.isNotEmpty() }
            .map { words ->
                when (words.size) {
                    0 -> "❓"
                    1 -> words.first()
                    2 -> words.joinToString("…")
                    else -> words.first() + "…" + words.last()
                }
            }
            .asStatus()

    /**
     * Contains all accessible files contained in this command line.
     */
    val includedFiles get() = commandLineParts.map { Path.of(it.unquoted) }.filter { it.exists }

    /**
     * Contains a formatted list of files contained in this command line.
     */
    val formattedIncludesFiles get() = includedFiles.joinToString("\n") { "📄 ${it.toUri()}" }

    override fun toString(): String = multiLineCommandLine

    val lines: List<String> get() = toString().lines()

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
     * Builds a proper script that runs this command line.
     *
     * @return temporary `.sh` file
     */
    fun toShellScript(): Path =
        ShellScript().apply {
            shebang
            changeDirectoryOrExit(directory = workingDirectory)
            command(commandLine)
        }.buildTo(Processes.tempScriptFile())

    /**
     * Prepares a new [ManagedProcess] that runs this command line as soon as it's triggered.
     *
     * @see execute
     */
    open fun prepare(expectedExitValue: Int = 0): ManagedProcess = toManagedProcess(expectedExitValue, null)

    /**
     * Starts a new [ManagedProcess] that runs this command line.
     */
    open fun execute(expectedExitValue: Int = 0): ManagedProcess = prepare(expectedExitValue).also { it.start() }

    /**
     * Starts a new [ManagedProcess] that runs this command line
     * and has it fully processed using `this` [RenderingLogger].
     */
    open fun RenderingLogger.executeLogging(
        caption: String,
        ansiCode: AnsiColorCode = ANSI.termColors.brightBlue,
        nonBlockingReader: Boolean = false,
        expectedExitValue: Int = 0,
    ): Int = logging(caption = caption, ansiCode = ansiCode) {
        execute(expectedExitValue).process(
            nonBlockingReader = nonBlockingReader,
            processInputStream = InputStream.nullInputStream(),
            processor = Processors.loggingProcessor(this))
    }.waitForTermination()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as CommandLine

        if (!commandLineParts.contentEquals(other.commandLineParts)) return false

        return true
    }

    override fun hashCode(): Int = commandLineParts.contentHashCode()
}
