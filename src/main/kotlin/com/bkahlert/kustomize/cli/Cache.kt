package com.bkahlert.kustomize.cli

import com.bkahlert.kustomize.libguestfs.ImageExtractor.extractImage
import koodies.io.path.FileSizeComparator
import koodies.io.path.cloneTo
import koodies.io.path.delete
import koodies.io.path.deleteRecursively
import koodies.io.path.extensionOrNull
import koodies.io.path.getSize
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.pathString
import koodies.io.path.requireDirectory
import koodies.io.path.uriString
import koodies.text.ANSI.Colors.cyan
import koodies.text.randomString
import koodies.time.Now
import koodies.time.format
import koodies.time.minutes
import koodies.time.parseInstant
import koodies.tracing.rendering.spanningLine
import koodies.tracing.spanning
import koodies.unit.BinaryPrefixes
import java.nio.file.Path
import java.time.Instant
import java.time.Instant.now
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.isHidden
import kotlin.io.path.isRegularFile
import kotlin.io.path.isWritable
import kotlin.io.path.moveTo
import kotlin.time.Duration

class Cache(dir: Path, private val maxConcurrentWorkingDirectories: Int = 5) : com.bkahlert.kustomize.cli.ManagedDirectory(
    com.bkahlert.kustomize.Kustomize.WorkingDirectory.resolve(dir).createDirectories().normalize(),
) {

    fun provideCopy(name: String, provider: () -> Path): Path =
        ProjectDirectory(dir, name, maxConcurrentWorkingDirectories).require(provider)
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


class ProjectDirectory(
    parentDir: Path,
    dirName: String,
    maxConcurrentWorkingDirectories: Int,
) : com.bkahlert.kustomize.cli.ManagedDirectory(parentDir, dirName) {

    constructor(dir: Path) : this(dir.parent, dir.fileName.pathString, Int.MAX_VALUE)

    init {
        deleteOldWorkDirs(maxConcurrentWorkingDirectories)
    }

    private val downloadDir = SingleFileDirectory(dir, "download")
    private val rawDir = SingleFileDirectory(dir, "raw")

    val workDirs: List<WorkDirectory>
        get() = dir.listDirectoryEntriesRecursively()
            .filter { WorkDirectory.isWorkDirectory(it) }
            .map { WorkDirectory(it) }
            .sortedBy { it.age }

    private fun deleteOldWorkDirs(keep: Int) =
        spanning("Deleting old working directories") {
            workDirs
                .filter { it.age > 30.minutes }
                .drop(keep)
                .forEach {
                    log("Removing ${it.age.toString().cyan()} old $it")
                    it.delete()
                }
        }

    fun require(provider: () -> Path): Path = spanning("Retrieving image") {
        val img = rawDir.requireSingle {
            val downloadedFile = downloadDir.requireSingle(provider)
            downloadedFile.extractImage()
        }

        WorkDirectory.from(dir, img).single { it.extensionOrNull.equals("img", ignoreCase = true) }
    }
}

private open class SingleFileDirectory(dir: Path) : com.bkahlert.kustomize.cli.ManagedDirectory(dir) {
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

    fun requireSingle(provide: () -> Path): Path {
        val file = getSingle()
        if (file != null) return file
        super.dir.createDirectories()
        return provide().let { providedFile ->
            val destFile = dir.resolve(providedFile.fileName)
            if (destFile.exists()) {
                providedFile.delete()
                destFile
            } else {
                spanningLine("Moving download to ${destFile.uriString}") {
                    providedFile.moveTo(destFile)
                }
            }
        }
    }

    override fun toString(): String {
        val count = fileCount().let { if (it == 1) "1 file" else "$it files" }
        return "SingleFileDirectory($count in $dir)"
    }
}

class WorkDirectory(dir: Path) : com.bkahlert.kustomize.cli.ManagedDirectory(dir.parent,
    requireValidName(dir)) {

    val age: Duration get() = Now.passedSince(createdAt(dir).toEpochMilli())

    fun single(predicate: (path: Path) -> Boolean = { true }): Path = dir
        .listDirectoryEntriesRecursively()
        .filterNot(Path::isHidden)
        .filter(predicate)
        .sortedWith(FileSizeComparator)
        .single()

    override fun toString(): String {
        val files = dir.listDirectoryEntriesRecursively().filter { it.isRegularFile() }
        return "working directory ${dir.fileName} containing ${files.size} file(s) / ${dir.getSize().toString(BinaryPrefixes)}"
    }

    companion object {
        private const val SEPARATOR = "--"

        fun requireValidName(dir: Path): String {
            createdAt(dir)
            return dir.fileName.pathString
        }

        private fun createdAt(dir: Path): Instant {
            val name = dir.fileName.pathString
            require(name.contains(SEPARATOR)) { "$name does not contain $SEPARATOR" }
            val potentialDateTime = name.substringBefore(SEPARATOR).trim()
            return potentialDateTime.parseInstant<Instant, Path>()
        }

        fun isWorkDirectory(dir: Path) = kotlin.runCatching {
            dir.requireDirectory()
            requireValidName(dir)
        }.isSuccess

        fun from(dir: Path, workFile: Path): WorkDirectory {
            val workDirectory =
                WorkDirectory(dir.resolve(now().format<Path>() + SEPARATOR + randomString(
                    4)))
            workFile.cloneTo(workDirectory.dir.resolve(workFile.fileName))
            return workDirectory
        }
    }
}
