package com.imgcstmzr.cli

import com.github.ajalt.clikt.output.TermUi.echo
import com.imgcstmzr.process.Unarchiver
import com.imgcstmzr.util.Paths
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant.now
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Cache(dir: Path = DEFAULT) : ManagedDirectory(dir) {

    fun provideCopy(name: String, reuseLastWorkingCopy: Boolean = false, provider: () -> Path): Path =
        ProjectDirectory(dir, name, reuseLastWorkingCopy).require(provider)

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


private class ProjectDirectory(parentDir: Path, dirName: String, val reuseLastWorkingCopy: Boolean) : ManagedDirectory(parentDir, dirName) {

    private val downloadDir = SingleFileDirectory(dir, "download")
    private val rawDir = SingleFileDirectory(dir, "raw")

    private fun workDirs(): List<WorkDirectory> {
        val listFiles: Array<File> = file.listFiles { dir, name ->
            WorkDirectory.isNameValid(name)
        } ?: return emptyList()

        return listFiles
            .sortedByDescending { it.lastModified() }
            .map { file -> WorkDirectory(file.toPath()) }
    }

    private fun deleteOldWorkDirs() {
        workDirs()
            .withIndex()
            .filter { it.index >= MAX_CONCURRENT_WORK_DIRS - 1 }
            .forEach { indexedValue ->
                echo("Removing old ${indexedValue.value}")
                indexedValue.value.delete()
            }
    }

    fun require(provider: () -> Path): Path {
        deleteOldWorkDirs()

        val downloadedFile = downloadDir.requireSingle(provider)
        val img = rawDir.requireSingle { Unarchiver.unarchive(downloadedFile) }

        val workDirs = workDirs()
        if (reuseLastWorkingCopy) {
            if (workDirs.isNotEmpty()) {
                val file = workDirs[0].getSingle()
                echo("Re-using last working copy ${file.fileName}")
                return file
            }
            echo("No working copy exists that could be re-used. Creating a new one.")
        }
        return WorkDirectory.from(dir, img).getSingle()
    }

    companion object {
        const val MAX_CONCURRENT_WORK_DIRS: Int = 3
    }
}

private open class SingleFileDirectory(dir: Path) : ManagedDirectory(dir) {
    constructor(parentDir: Path, dirName: String) : this(parentDir.resolve(dirName))

    private fun fileCount(): Int = file.list(DEFAULT_FILTER)?.size ?: 0

    fun fileExists(): Boolean = file.exists() && file.isDirectory && fileCount() == 1

    fun getSingle(): Path? =
        if (fileExists()) file.listFiles(DEFAULT_FILTER)?.get(0)?.toPath() else null

    fun requireSingle(provider: () -> Path): Path {
        val file = getSingle()
        if (file != null) return file
        super.file.mkdirs()
        return provider().let {
            Files.move(it, dir.resolve(it.fileName))
        }
    }

    override fun toString(): String {
        val count = fileCount().let { if (it == 1) "1 file" else "$it files" }
        return "SingleFileDirectory($count in $dir)"
    }


    companion object {
        val DEFAULT_FILTER: (File, String) -> Boolean = { dir: File, name: String ->
            !name.startsWith(".")
        }
    }
}

private class WorkDirectory(dir: Path) : ManagedDirectory(dir.parent, requireValidName(dir)) {

    fun getSingle(): Path {
        return file.listFiles { dir: File, name: String ->
            name.endsWith(".img")
        }?.single()?.toPath() ?: throw NoSuchElementException("$dir does not contain any .img file")
    }

    override fun toString(): String = "WorkDirectory(${getSingle()})"

    companion object {
        private val DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss--n").withZone(ZoneId.systemDefault())

        fun requireValidName(dir: Path): String {
            val name = dir.fileName.toString()
            return kotlin.runCatching { DATE_FORMATTER.parse(name) }.mapCatching { name }.getOrThrow()
        }

        fun isNameValid(name: String) = kotlin.runCatching { DATE_FORMATTER.parse(name) }.isSuccess

        fun from(dir: Path, file: Path): WorkDirectory {
            val workDirectory = WorkDirectory(dir.resolve(DATE_FORMATTER.format(now())))
            Files.copy(file, workDirectory.dir.resolve(file.fileName))
            return workDirectory
        }
    }
}


