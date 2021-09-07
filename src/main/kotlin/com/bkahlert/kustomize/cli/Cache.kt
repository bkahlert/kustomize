package com.bkahlert.kustomize.cli

import com.bkahlert.kommons.io.path.FileSizeComparator
import com.bkahlert.kommons.io.path.age
import com.bkahlert.kommons.io.path.cloneTo
import com.bkahlert.kommons.io.path.delete
import com.bkahlert.kommons.io.path.deleteRecursively
import com.bkahlert.kommons.io.path.extensionOrNull
import com.bkahlert.kommons.io.path.getSize
import com.bkahlert.kommons.io.path.listDirectoryEntriesRecursively
import com.bkahlert.kommons.io.path.pathString
import com.bkahlert.kommons.io.path.requireDirectory
import com.bkahlert.kommons.io.path.uriString
import com.bkahlert.kommons.text.ANSI.Colors.cyan
import com.bkahlert.kommons.text.randomString
import com.bkahlert.kommons.time.Now
import com.bkahlert.kommons.time.days
import com.bkahlert.kommons.time.format
import com.bkahlert.kommons.time.minutes
import com.bkahlert.kommons.time.parseInstant
import com.bkahlert.kommons.tracing.rendering.runSpanningLine
import com.bkahlert.kommons.tracing.runSpanning
import com.bkahlert.kommons.tracing.spanScope
import com.bkahlert.kommons.unit.BinaryPrefixes
import com.bkahlert.kustomize.Kustomize
import com.bkahlert.kustomize.libguestfs.ImageExtractor.extractImage
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

class Cache(
    dir: Path,
    private val maxConcurrentWorkingDirectories: Int = 5,
    private val maxAge: Duration = 10.days,
) : ManagedDirectory(
    Kustomize.work.resolve(dir).createDirectories().normalize(),
) {

    fun provideCopy(name: String, provider: () -> Path): Path =
        ProjectDirectory(dir, name, maxConcurrentWorkingDirectories, maxAge).require(provider)
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
    maxAge: Duration,
) : ManagedDirectory(parentDir, dirName) {

    init {
        deleteOldWorkDirs(maxConcurrentWorkingDirectories)
    }

    private val downloadDir = SingleFileDirectory(dir, "download", maxAge)
    private val rawDir = SingleFileDirectory(dir, "raw", maxAge)

    val workDirs: List<WorkDirectory>
        get() = dir.listDirectoryEntriesRecursively()
            .filter { WorkDirectory.isWorkDirectory(it) }
            .map { WorkDirectory(it) }
            .sortedBy { it.age }

    private fun deleteOldWorkDirs(keep: Int) =
        runSpanning("Deleting old working directories") {
            workDirs
                .filter { it.age > 30.minutes }
                .drop(keep)
                .forEach {
                    log("Removing ${it.age.toString().cyan()} old $it")
                    it.delete()
                }
        }

    fun require(provider: () -> Path): Path = runSpanning("Retrieving image") {
        val img = rawDir.single {
            val downloadedFile = downloadDir.single(provider)
            downloadedFile.extractImage()
        }

        WorkDirectory.from(dir, img).single { it.extensionOrNull.equals("img", ignoreCase = true) }
    }
}

private open class SingleFileDirectory(dir: Path, val maxAge: Duration) : ManagedDirectory(dir) {
    constructor(parentDir: Path, dirName: String, maxAge: Duration) : this(parentDir.resolve(dirName), maxAge)

    private val files: Sequence<Path>
        get() = spanScope {
            if (!dir.exists() || !dir.isDirectory()) emptySequence()
            else dir.listDirectoryEntriesRecursively()
                .asSequence()
                .filterNot(Path::isHidden)
                .filter { it != dir }
                .mapNotNull { file ->
                    if (file.age > maxAge) {
                        log("${file.fileName} is outdated. Deleting...")
                        file.delete()
                        null
                    } else file
                }
                .sortedWith(FileSizeComparator)
        }

    val fileExists: Boolean get() = files.count() > 0

    fun singleOrNull(): Path? = files.singleOrNull()

    fun single(provide: () -> Path): Path {
        val file = singleOrNull()
        if (file != null) return file
        super.dir.createDirectories()
        return provide().let { providedFile ->
            val destFile = dir.resolve(providedFile.fileName)
            if (destFile.exists()) {
                providedFile.delete()
                destFile
            } else {
                runSpanningLine("Moving download to ${destFile.uriString}") {
                    providedFile.moveTo(destFile)
                }
            }
        }
    }

    override fun toString(): String {
        val count = files.count().let { if (it == 1) "1 file" else "$it files" }
        return "SingleFileDirectory($count in $dir)"
    }
}

class WorkDirectory(dir: Path) : ManagedDirectory(dir.parent,
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
