package com.imgcstmzr.cli

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.patch.ini.RegexElement
import com.imgcstmzr.process.Unarchiver.unarchive
import com.imgcstmzr.util.Paths
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.exists
import com.imgcstmzr.util.extension
import com.imgcstmzr.util.isFile
import com.imgcstmzr.util.joinToTruncatedString
import com.imgcstmzr.util.listFilesRecursively
import com.imgcstmzr.util.random
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant.now
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ofPattern

class Cache(dir: Path = DEFAULT, private val maxConcurrentWorkingDirectories: Int = 3) : ManagedDirectory(dir) {

    fun provideCopy(name: String, reuseLastWorkingCopy: Boolean = false, provider: () -> Path): Path =
        ProjectDirectory(dir, name, reuseLastWorkingCopy, maxConcurrentWorkingDirectories).require(provider)

    companion object {
        val DEFAULT: Path = Paths.USER_HOME.resolve(".imgcstmzr")
    }
}

open class ManagedDirectory(val dir: Path) {
    constructor(parentDir: Path, dirName: String) : this(parentDir.resolve(dirName))

    protected val file: File = dir.toAbsolutePath().toFile()

    init {
        if (!file.exists()) {
            file.mkdirs()
            check(file.exists()) { "$dir does not exist and could not be created." }
        }
        check(file.canWrite()) { "Cannot write to $dir" }
    }

    private val absDir: String = file.toString()

    fun delete() {
        if (file.exists()) file.deleteRecursively()
    }
}


private class ProjectDirectory(parentDir: Path, dirName: String, private val reuseLastWorkingCopy: Boolean, private val maxConcurrentWorkingDirectories: Int) :
    ManagedDirectory(parentDir, dirName) {

    private val downloadDir = SingleFileDirectory(dir, "download")
    private val rawDir = SingleFileDirectory(dir, "raw")

    private fun workDirs(): List<WorkDirectory> {
        val listFiles: Array<File> = file.listFiles { dir, name ->
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

    fun require(provider: () -> Path): Path {
        deleteOldWorkDirs()

        val workDirs = workDirs()
        if (reuseLastWorkingCopy) {
            val lastWorkingCopy = workDirs.mapNotNull { workDir ->
                val single = workDir.getSingle { file ->
                    file.extension == "img"
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
            downloadedFile.takeIf { it.extension == "img" } ?: unarchive(downloadedFile)
        }

        return WorkDirectory.from(dir, img).getSingle { it.extension == "img" } ?: throw NoSuchElementException()
    }
}

private open class SingleFileDirectory(dir: Path) : ManagedDirectory(dir) {
    constructor(parentDir: Path, dirName: String) : this(parentDir.resolve(dirName))

    private fun fileCount(): Int = dir.listFilesRecursively(DEFAULT_FILTER(dir)).size

    fun fileExists(): Boolean = file.exists() && file.isDirectory && fileCount() > 0

    fun getSingle(): Path? =
        if (fileExists()) {
            dir.listFilesRecursively(DEFAULT_FILTER(dir), Paths.fileSizeComparator)[0]
        } else null

    fun requireSingle(provider: () -> Path): Path {
        val file = getSingle()
        if (file != null) return file
        super.file.mkdirs()
        return provider().let { costlyProvidedFile ->
            val destFile = dir.resolve(costlyProvidedFile.fileName)
            if (destFile.exists) {
                costlyProvidedFile.delete()
                destFile
            } else Files.move(costlyProvidedFile, destFile)
        }
    }

    override fun toString(): String {
        val count = fileCount().let { if (it == 1) "1 file" else "$it files" }
        return "SingleFileDirectory($count in $dir)"
    }
}

private class WorkDirectory(dir: Path) : ManagedDirectory(dir.parent, requireValidName(dir)) {
    private class WorkDirectoryName(name: String) : RegexElement(name, false) {
        constructor() : this(DATE_FORMATTER.format(now()) + SEPARATOR + String.random(4))

        companion object {
            private val DATE_FORMATTER = ofPattern("yyyy-MM-dd'T'HH-mm-ss").withZone(ZoneId.systemDefault())
            private val SEPARATOR = "--"
        }

        private val iso by regex("^\\d{4}-\\d{2}-\\d{2}T\\d{2}-\\d{2}-\\d{2}")
        val dateTime get() = DATE_FORMATTER.parse(iso)
        val separator by regex(SEPARATOR)
        val random by regex("\\w+")
    }

    fun getSingle(filter: (path: Path) -> Boolean): Path? =
        dir.listFilesRecursively({
            !it.fileName.startsWith(".") && filter(it)
        }, Paths.fileSizeComparator).firstOrNull()

    override fun toString(): String {
        val files = dir.listFilesRecursively({ it.isFile })
        return StringBuilder("WorkDirectory(").append(dir.fileName)
            .append(" containing ").append(files.size).append(" file(s)")
            .also { sb -> if (files.isNotEmpty()) sb.append(": ").append(dir.listFilesRecursively({ it.isFile }).joinToTruncatedString()) }
            .append(")")
            .toString()
    }

    companion object {
        fun requireValidName(dir: Path): String = kotlin.runCatching { WorkDirectoryName(dir.fileName.toString()).toString() }.getOrThrow()
        fun isNameValid(name: String) = kotlin.runCatching { WorkDirectoryName(name).toString() }.isSuccess

        fun from(dir: Path, file: Path): WorkDirectory {
            val workDirectory = WorkDirectory(dir.resolve(WorkDirectoryName().toString()))
            Files.copy(file, workDirectory.dir.resolve(file.fileName))
            return workDirectory
        }
    }
}

private fun DEFAULT_FILTER(currentPath: Path) = { path: Path -> !path.fileName.startsWith(".") && path != currentPath }
