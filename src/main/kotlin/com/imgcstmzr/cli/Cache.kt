package com.imgcstmzr.cli

import com.bkahlert.koodies.nio.file.cloneTo
import com.bkahlert.koodies.nio.file.delete
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.extensionOrNull
import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.string.random
import com.bkahlert.koodies.unit.Size
import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.cli.Cache.Companion.defaultFilter
import com.imgcstmzr.guestfish.ImageBuilder.buildFrom
import com.imgcstmzr.guestfish.ImageExtractor.extractImage
import com.imgcstmzr.patch.ini.RegexElement
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.runtime.log.RenderingLogger
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.isFile
import com.imgcstmzr.util.joinToTruncatedString
import com.imgcstmzr.util.listFilesRecursively
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant.now
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ofPattern
import java.time.temporal.TemporalAccessor

class Cache(dir: Path = DEFAULT, private val maxConcurrentWorkingDirectories: Int = 3) : ManagedDirectory(dir) {

    fun BlockRenderingLogger.provideCopy(name: String, reuseLastWorkingCopy: Boolean = false, provider: () -> Path): Path =
        with(ProjectDirectory(dir, name, reuseLastWorkingCopy, maxConcurrentWorkingDirectories)) {
            require(provider)
        }

    companion object {
        val DEFAULT: Path = Paths.USER_HOME.resolve(".imgcstmzr")
        fun defaultFilter(currentPath: Path): (Path) -> Boolean = { path: Path -> !path.fileName.startsWith(".") && path != currentPath }
    }
}

open class ManagedDirectory(val dir: Path) {
    constructor(parentDir: Path, dirName: String) : this(parentDir.resolve(dirName))

    protected val file: File = dir.toAbsolutePath().toFile() // TODO remove toFile

    init {
        if (!file.exists()) {
            file.mkdirs()
            check(file.exists()) { "$dir does not exist and could not be created." }
        }
        check(file.canWrite()) { "Cannot write to $dir" }
    }

    fun delete() {
        if (file.exists()) file.deleteRecursively()
    }
}


private class ProjectDirectory(parentDir: Path, dirName: String, private val reuseLastWorkingCopy: Boolean, private val maxConcurrentWorkingDirectories: Int) :
    ManagedDirectory(parentDir, dirName) {

    private val downloadDir = SingleFileDirectory(dir, "download")
    private val rawDir = SingleFileDirectory(dir, "raw")

    private fun workDirs(): List<WorkDirectory> {
        val listFiles: Array<File> = file.listFiles { _, name ->
            WorkDirectory.isNameValid(name)
        } ?: return emptyList()

        return listFiles
            .filter { it.isDirectory }
            .sortedByDescending { it.lastModified() }
            .map { file -> WorkDirectory(file.toPath()) }
    }

    private fun deleteOldWorkDirs() {
        workDirs()
            .withIndex()
            .filter { it.index >= maxConcurrentWorkingDirectories - 1 }
            .forEach { indexedValue ->
                echo("Removing old ${indexedValue.value}")
                indexedValue.value.delete()
            }
    }

    fun RenderingLogger.require(provider: () -> Path): Path {
        deleteOldWorkDirs()

        val workDirs = workDirs()
        if (reuseLastWorkingCopy) {
            val lastWorkingCopy = workDirs.mapNotNull { workDir ->
                val single = workDir.getSingle { file ->
                    file.extensionOrNull == "img"
                }
                single
            }.firstOrNull()
            if (lastWorkingCopy != null) {
                echo("Re-using last working copy ${lastWorkingCopy.fileName}")
                return lastWorkingCopy
            }
            echo("No working copy exists that could be re-used. Creating a new one.")
        }

        val img = rawDir.requireSingle {
            val downloadedFile = downloadDir.requireSingle(provider)
            downloadedFile.extractImage { path -> buildFrom(path) }
        }

        return WorkDirectory.from(dir, img).getSingle { it.extensionOrNull.equals("img", ignoreCase = true) } ?: throw NoSuchElementException()
    }
}

private open class SingleFileDirectory(dir: Path) : ManagedDirectory(dir) {
    constructor(parentDir: Path, dirName: String) : this(parentDir.resolve(dirName))

    private fun fileCount(): Int = dir.listFilesRecursively(defaultFilter(dir)).size

    fun fileExists(): Boolean = file.exists() && file.isDirectory && fileCount() > 0

    fun getSingle(): Path? =
        if (fileExists()) {
            dir.listFilesRecursively(defaultFilter(dir), Size.FileSizeComparator)[0]
        } else null

    fun requireSingle(provider: () -> Path): Path {
        val file = getSingle()
        if (file != null) return file
        super.file.mkdirs()
        return provider().let { costlyProvidedFile ->
            val destFile = dir.resolve(costlyProvidedFile.fileName)
            if (destFile.exists) {
                costlyProvidedFile.delete(false)
                destFile
            } else {
                echo("Moving file to $destFile...", trailingNewline = false)
                Files.move(costlyProvidedFile, destFile).also { echo(" Completed.") }
            }
        }
    }

    override fun toString(): String {
        val count = fileCount().let { if (it == 1) "1 file" else "$it files" }
        return "SingleFileDirectory($count in $dir)"
    }
}

private class WorkDirectory(dir: Path) : ManagedDirectory(dir.parent, requireValidName(dir)) {
    @Suppress("unused")
    private class WorkDirectoryName(name: String) : RegexElement(name, false) {
        constructor() : this(DATE_FORMATTER.format(now()) + SEPARATOR + String.random(4))

        companion object {
            @Suppress("SpellCheckingInspection")
            private val DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH-mm-ss").withZone(ZoneId.systemDefault())
            private const val SEPARATOR = "--"
        }

        private val iso by regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}-\\d{2}-\\d{2}")
        val dateTime: TemporalAccessor get() = DATE_FORMATTER.parse(iso)
        val separator by regex(SEPARATOR)
        val random by regex("\\w+")
    }

    fun getSingle(filter: (path: Path) -> Boolean): Path? =
        dir.listFilesRecursively({
            !it.fileName.startsWith(".") && filter(it)
        }, Size.FileSizeComparator).firstOrNull()

    override fun toString(): String {
        val files = dir.listFilesRecursively({ it.isFile })
        return StringBuilder("WorkDirectory(").append(dir.fileName)
            .append(" containing ").append(files.size).append(" file(s)")
            .also { sb -> if (files.isNotEmpty()) sb.append(": ").append(dir.listFilesRecursively({ it.isFile }).joinToTruncatedString()) }
            .append(")")
            .toString()
    }

    companion object {
        fun requireValidName(dir: Path): String = kotlin.runCatching { WorkDirectoryName(dir.fileName.serialized).toString() }.getOrThrow()
        fun isNameValid(name: String) = kotlin.runCatching {
            val workDirectoryName = WorkDirectoryName(name)
            workDirectoryName.toString()
        }.isSuccess

        fun from(dir: Path, file: Path): WorkDirectory {
            val workDirectory = WorkDirectory(dir.resolve(WorkDirectoryName().toString()))
            file.cloneTo(workDirectory.dir.resolve(file.fileName))
            return workDirectory
        }
    }
}

