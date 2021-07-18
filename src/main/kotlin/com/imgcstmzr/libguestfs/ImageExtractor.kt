package com.imgcstmzr.libguestfs

import com.imgcstmzr.ImgCstmzr
import koodies.io.compress.Archiver.listArchive
import koodies.io.compress.Archiver.unarchive
import koodies.io.path.extensionOrNull
import koodies.io.path.getSize
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.moveToDirectory
import koodies.io.path.uriString
import koodies.io.selfCleaning
import koodies.io.tempDir
import koodies.time.hours
import koodies.tracing.spanning
import java.nio.file.Path

object ImageExtractor {

    private val temp by ImgCstmzr.Temp.resolve("image-extract").selfCleaning(1.hours, 5)

    val imgFilter: (Path) -> Boolean = { path ->
        path.extensionOrNull.equals("img", ignoreCase = true)
            && !path.startsWith("__MACOSX")
            && !path.last().toString().startsWith("._")
    }

    fun Path.extractImage(imageBuilder: (Path) -> Path): Path = spanning("Unarchiving $uriString (${getSize()})") {
        if (imgFilter(this@extractImage)) {
            log("$fileName is already an image.")
            this@extractImage
        } else {
            val temp = temp.tempDir("extract-")

            val filteredArchiveEntries = filterArchiveEntries()
            when (filteredArchiveEntries.size) {
                0 -> {
                    log("No image found. Using archive as image.")
                    imageBuilder(this@extractImage).moveToDirectory(temp)
                }
                1 -> {
                    spanning("Extracting found image ${filteredArchiveEntries.first().name}") {
                        unarchive(temp)
                    }
                }
                else -> {
                    spanning("Multiple image candidates (${filteredArchiveEntries.joinToString { it.name }}) found. Extracting largest candidate") {
                        unarchive(temp)
                    }
                }
            }

            temp.listDirectoryEntriesRecursively()
                .filter { imgFilter.invoke(it) }
                .maxByOrNull { it.getSize() }
                ?: throw IllegalStateException("An unknown error occurred while unarchiving. $temp was supposed to contain the unarchived file but was empty.")
        }
    }

    private fun Path.filterArchiveEntries() = listArchive().also {
        require(it.isNotEmpty()) { "Cannot extract image from archive $this as it's empty." }
    }.let { list -> list.filter { entry -> imgFilter(Path.of(entry.name)) } }
}
