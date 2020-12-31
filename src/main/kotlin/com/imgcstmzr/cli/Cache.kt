package com.imgcstmzr.cli

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.cli.Cache.Companion.defaultFilter
import com.imgcstmzr.libguestfs.docker.ImageBuilder.buildFrom
import com.imgcstmzr.libguestfs.docker.ImageExtractor.extractImage
import com.imgcstmzr.patch.ini.RegexElement
import com.imgcstmzr.util.Paths
import koodies.io.path.age
import koodies.io.path.asString
import koodies.io.path.cloneTo
import koodies.io.path.delete
import koodies.io.path.deleteRecursively
import koodies.io.path.extensionOrNull
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.logging.BlockRenderingLogger
import koodies.logging.RenderingLogger
import koodies.text.randomString
import koodies.unit.Size
import koodies.unit.size
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant.now
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ofPattern
import java.time.temporal.TemporalAccessor
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import kotlin.time.minutes

class Cache(dir: Path = DEFAULT, private val maxConcurrentWorkingDirectories: Int = 5) : ManagedDirectory(dir) {

    fun BlockRenderingLogger.provideCopy(name: String, reuseLastWorkingCopy: Boolean = false, provider: () -> Path): Path =
        with(ProjectDirectory(dir, name, reuseLastWorkingCopy, maxConcurrentWorkingDirectories)) {
            require(provider)
        }

    companion object {
        val DEFAULT: Path = Paths.CACHE
        fun defaultFilter(currentPath: Path): (Path) -> Boolean = { path: Path -> !path.fileName.startsWith(".") && path != currentPath }
    }
}

open class ManagedDirectory(val dir: Path) {
    constructor(parentDir: Path, dirName: String) : this(parentDir.resolve(dirName))

    init {
        if (!dir.exists()) {
            dir.createDirectories()
            check(dir.exists()) { "$dir does not exist and could not be created." }
        }
        check(dir.isWritable()) { "Cannot write to $dir" }
    }

    fun delete() {
        if (dir.exists()) dir.deleteRecursively()
    }
}


private class ProjectDirectory(parentDir: Path, dirName: String, private val reuseLastWorkingCopy: Boolean, maxConcurrentWorkingDirectories: Int) :
    ManagedDirectory(parentDir, dirName) {

    init {
        deleteOldWorkDirs(maxConcurrentWorkingDirectories)
    }

    private val downloadDir = SingleFileDirectory(dir, "download")
    private val rawDir = SingleFileDirectory(dir, "raw")

    private fun workDirs(): List<WorkDirectory> {
        val listFiles: List<Path> = dir.listDirectoryEntriesRecursively().filter {
            WorkDirectory.isNameValid(it.fileName.asString())
        }

        return listFiles
            .filter { it.isDirectory() && it.age > 30.minutes }
            .sortedByDescending { it.getLastModifiedTime() }
            .map { WorkDirectory(it) }
    }

    private fun deleteOldWorkDirs(keep: Int) {
        workDirs()
            .withIndex()
            .filter { it.index >= keep - 1 }
            .forEach { (i, dir) ->
                echo("Removing old $dir")
                dir.delete()
            }
    }

    fun RenderingLogger.require(provider: () -> Path): Path {
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

    private fun fileCount(): Int = dir.listDirectoryEntriesRecursively().filter(defaultFilter(dir)).size

    fun fileExists(): Boolean = dir.exists() && dir.isDirectory() && fileCount() > 0

    fun getSingle(): Path? =
        if (fileExists()) {
            dir.listDirectoryEntriesRecursively().filter(defaultFilter(dir)).sortedWith(Size.FileSizeComparator)[0]
        } else null

    fun requireSingle(provider: () -> Path): Path {
        val file = getSingle()
        if (file != null) return file
        super.dir.createDirectories()
        return provider().let { costlyProvidedFile ->
            val destFile = dir.resolve(costlyProvidedFile.fileName)
            if (destFile.exists()) {
                costlyProvidedFile.delete()
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
        constructor() : this(DATE_FORMATTER.format(now()) + SEPARATOR + randomString(4))

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
        dir.listDirectoryEntriesRecursively().filter {
            !it.fileName.startsWith(".") && filter(it)
        }.sortedWith(Size.FileSizeComparator).firstOrNull()

    override fun toString(): String {
        val files = dir.listDirectoryEntriesRecursively().filter { it.isRegularFile() }
        return "WorkDirectory(${dir.fileName} containing ${files.size} file(s) / ${dir.size})"
    }

    companion object {
        fun requireValidName(dir: Path): String = kotlin.runCatching { WorkDirectoryName(dir.fileName.asString()).toString() }.getOrThrow()
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

