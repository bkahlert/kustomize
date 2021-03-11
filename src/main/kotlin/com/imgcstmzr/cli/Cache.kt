package com.imgcstmzr.cli

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.libguestfs.docker.ImageBuilder.buildFrom
import com.imgcstmzr.libguestfs.docker.ImageExtractor.extractImage
import com.imgcstmzr.util.Paths
import koodies.io.path.FileSizeComparator
import koodies.io.path.asString
import koodies.io.path.cloneTo
import koodies.io.path.delete
import koodies.io.path.deleteRecursively
import koodies.io.path.extensionOrNull
import koodies.io.path.getSize
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.requireDirectory
import koodies.logging.RenderingLogger
import koodies.text.ANSI.Colors.cyan
import koodies.text.randomString
import koodies.time.Now
import koodies.time.format
import koodies.time.parseInstant
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.time.Instant.now
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import kotlin.time.Duration
import kotlin.time.minutes

class Cache(dir: Path = DEFAULT, private val maxConcurrentWorkingDirectories: Int = 5) : ManagedDirectory(dir) {

    fun RenderingLogger.provideCopy(name: String, reuseLastWorkingCopy: Boolean = false, provider: () -> Path): Path =
        with(ProjectDirectory(dir, name, this, reuseLastWorkingCopy, maxConcurrentWorkingDirectories)) {
            require(provider)
        }

    companion object {
        val DEFAULT: Path = Paths.CACHE
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
        dir.deleteRecursively()
    }
}


private class ProjectDirectory(
    parentDir: Path,
    dirName: String,
    private val logger: RenderingLogger,
    private val reuseLastWorkingCopy: Boolean,
    maxConcurrentWorkingDirectories: Int,
) :
    ManagedDirectory(parentDir, dirName) {

    init {
        deleteOldWorkDirs(maxConcurrentWorkingDirectories)
    }

    private val downloadDir = SingleFileDirectory(dir, "download")
    private val rawDir = SingleFileDirectory(dir, "raw")

    private fun workDirs(): List<WorkDirectory> {

        return dir.listDirectoryEntriesRecursively()
            .filter { WorkDirectory.isWorkDirectory(it) }
            .map { WorkDirectory(it) }
            .sortedBy { it.age }
    }

    private fun deleteOldWorkDirs(keep: Int) = workDirs()
        .filter { it.age > 30.minutes }
        .drop(keep)
        .forEach {
            logger.logLine { "Removing ${it.age.toString().cyan()} old $it" }
            it.delete()
        }

    fun RenderingLogger.require(provider: () -> Path): Path {
        val workDirs = workDirs()
        val workDir: Path? = if (reuseLastWorkingCopy) {
            workDirs.mapNotNull { workDir ->
                val single = workDir.getSingle { file ->
                    file.extensionOrNull == "img"
                }
                single
            }.firstOrNull().also {
                if (it != null) echo("Re-using last working copy ${it.fileName}")
                else echo("No working copy exists that could be re-used. Creating a new one.")
            }
        } else null

        return workDir ?: run {

            val img = rawDir.requireSingle {
                val downloadedFile = downloadDir.requireSingle(provider)
                downloadedFile.extractImage { path -> buildFrom(path) }
            }

            WorkDirectory.from(dir, img).getSingle { it.extensionOrNull.equals("img", ignoreCase = true) } ?: throw NoSuchElementException()
        }
    }
}

private open class SingleFileDirectory(dir: Path) : ManagedDirectory(dir) {
    constructor(parentDir: Path, dirName: String) : this(parentDir.resolve(dirName))

    private fun fileCount(): Int = dir
        .listDirectoryEntriesRecursively()
        .filterNot(Path::isHidden)
        .filter { it != dir }
        .size

    fun fileExists(): Boolean = dir.exists() && dir.isDirectory() && fileCount() > 0

    fun getSingle(): Path? =
        if (fileExists()) dir
            .listDirectoryEntriesRecursively()
            .filterNot(Path::isHidden)
            .filter { it != dir }
            .sortedWith(FileSizeComparator)[0]
        else null

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
                echo("Moving file to $destFile …", trailingNewline = false)
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

    val age: Duration get() = Now.passedSince(createdAt(dir).toEpochMilli())

    fun getSingle(filter: (path: Path) -> Boolean): Path? = dir
        .listDirectoryEntriesRecursively()
        .filterNot(Path::isHidden)
        .filter(filter)
        .sortedWith(FileSizeComparator)
        .firstOrNull()

    override fun toString(): String {
        val files = dir.listDirectoryEntriesRecursively().filter { it.isRegularFile() }
        return "working directory ${dir.fileName} containing ${files.size} file(s) / ${dir.getSize()}"
    }

    companion object {
        private const val SEPARATOR = "--"

        fun requireValidName(dir: Path): String {
            createdAt(dir)
            return dir.fileName.asString()
        }

        private fun createdAt(dir: Path): Instant {
            val name = dir.fileName.asString()
            require(name.contains(SEPARATOR)) { "$name does not contain $SEPARATOR" }
            val potentialDateTime = name.substringBefore(SEPARATOR).trim()
            return potentialDateTime.parseInstant<Instant, Path>()
        }

        fun isWorkDirectory(dir: Path) = kotlin.runCatching {
            dir.requireDirectory()
            requireValidName(dir)
        }.isSuccess

        fun from(dir: Path, workFile: Path): WorkDirectory {
            val workDirectory = WorkDirectory(dir.resolve(now().format<Path>() + SEPARATOR + randomString(4)))
            workFile.cloneTo(workDirectory.dir.resolve(workFile.fileName))
            return workDirectory
        }
    }
}

