package com.imgcstmzr

import com.github.ajalt.clikt.output.TermUi.echo
import java.io.File
import java.nio.file.Files
import java.time.Instant.now
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class Cache(dir: File = DEFAULT) : ManagedDirectory(dir) {

    fun provideCopy(name: String, reuseLastWorkingCopy: Boolean = false, provider: () -> File): File =
        ProjectDirectory(dir, name, reuseLastWorkingCopy).require(provider)

    companion object {
        val USER_HOME: File = File(System.getProperty("user.home"))
        val DEFAULT: File = File(USER_HOME, ".imgcstmzr")
    }
}

open class ManagedDirectory(val dir: File) {
    constructor(parentDir: File, dirName: String) : this(File(parentDir, dirName))

    init {
        if (!dir.exists()) {
            dir.mkdirs()
            check(dir.exists()) { "$dir does not exist and could not be created." }
        }
        check(dir.canWrite()) { "Cannot write to $dir" }
    }

    val absDir = dir.absolutePath.toString()

    fun delete() {
        if (dir.exists()) dir.deleteRecursively()
    }
}


private class ProjectDirectory(parentDir: File, dirName: String, val reuseLastWorkingCopy: Boolean) : ManagedDirectory(parentDir, dirName) {

    private val downloadDir = SingleFileDirectory(dir, "download")
    private val rawDir = SingleFileDirectory(dir, "raw")

    private fun workDirs(): List<WorkDirectory> {
        val listFiles: Array<File> = dir.listFiles { dir, name ->
            WorkDirectory.isNameValid(name)
        } ?: return emptyList()

        return listFiles
            .sortedByDescending { it.lastModified() }
            .map { file -> WorkDirectory(file) }
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

    fun require(provider: () -> File): File {
        deleteOldWorkDirs()

        val downloadedFile = downloadDir.requireFile(provider)
        val img = rawDir.requireFile {
            Unarchiver().unarchive(downloadedFile)
        }

        val workDirs = workDirs()
        if (reuseLastWorkingCopy) {
            if (workDirs.isNotEmpty()) {
                val file = workDirs[0].getFile()
                echo("Re-use last working copy $file")
                return file
            }
            echo("No working copy exists that could be re-used. Creating a new one.")
        }
        return WorkDirectory.from(dir, img).getFile()
    }

    companion object {
        const val MAX_CONCURRENT_WORK_DIRS: Int = 3
    }
}

private open class SingleFileDirectory(dir: File) : ManagedDirectory(dir) {
    constructor(parentDir: File, dirName: String) : this(File(parentDir, dirName))

    private fun fileCount(): Int = dir.list(DEFAULT_FILTER)?.size ?: 0

    fun fileExists(): Boolean = dir.exists() && dir.isDirectory && fileCount() == 1

    fun getFile(): File? =
        if (fileExists()) dir.listFiles(DEFAULT_FILTER)?.get(0) else null

    fun requireFile(provider: () -> File): File {
        val file = getFile()
        if (file != null) return file
        dir.mkdirs()
        return provider().toPath().let {
            Files.move(it, dir.toPath().resolve(it.fileName)).toFile()
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

private class WorkDirectory(dir: File) : ManagedDirectory(dir.parentFile, requireValidName(dir)) {

    fun getFile(): File {
        return dir.listFiles { dir: File, name: String ->
            name.endsWith(".img")
        }?.single() ?: throw NoSuchElementException("$dir does not contain any .img file")
    }

    override fun toString(): String {
        return "WorkDirectory(${getFile()})"
    }

    companion object {
        private val DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH-mm-ss--n").withZone(ZoneId.systemDefault())

        fun requireValidName(dir: File): String {
            val name = dir.name
            return kotlin.runCatching { DATE_FORMATTER.parse(name) }.mapCatching { name }.getOrThrow()
        }

        fun isNameValid(name: String) = kotlin.runCatching { DATE_FORMATTER.parse(name) }.isSuccess

        fun from(dir: File, file: File): WorkDirectory {
            val workDirectory = WorkDirectory(File(dir, DATE_FORMATTER.format(now())))
            Files.copy(file.toPath(), workDirectory.dir.toPath().resolve(file.toPath().fileName))
            return workDirectory
        }
    }
}


