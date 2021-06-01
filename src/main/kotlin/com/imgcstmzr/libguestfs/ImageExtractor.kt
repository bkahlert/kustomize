package com.imgcstmzr.libguestfs

import com.imgcstmzr.Locations
import koodies.io.autoCleaning
import koodies.io.compress.Archiver.listArchive
import koodies.io.compress.Archiver.unarchive
import koodies.io.path.extensionOrNull
import koodies.io.path.getSize
import koodies.io.path.listDirectoryEntriesRecursively
import koodies.io.path.moveToDirectory
import koodies.io.tempDir
import koodies.logging.FixedWidthRenderingLogger
import koodies.time.hours
import java.nio.file.Path

object ImageExtractor {

    private val temp by Locations.Temp.autoCleaning("image-extract", 1.hours, 5)

    val imgFilter: (Path) -> Boolean = { path ->
        path.extensionOrNull.equals("img", ignoreCase = true)
            && !path.startsWith("__MACOSX")
            && !path.last().toString().startsWith("._")
    }

    fun Path.extractImage(logger: FixedWidthRenderingLogger, imageBuilder: (Path) -> Path): Path {
        if (imgFilter(this)) {
            logger.logLine { "No need for extraction as $fileName seems to already be an image." }
            return this
        }

        val temp = temp.tempDir("extract-")
        return logger.compactLogging("Unarchiving $fileName (${getSize()}) …") {

            val filteredArchiveEntries = filterArchiveEntries()
            when (filteredArchiveEntries.size) {
                0 -> {
                    logLine { "No image found. Considering the archive the image itself …" }
                    imageBuilder(this@extractImage).moveToDirectory(temp)
                }
                1 -> {
                    logLine { "Image ${filteredArchiveEntries.first().name} found. Extracting …" }
                    unarchive(temp)
                }
                else -> {
                    logLine { "Multiple image candidates (${filteredArchiveEntries.joinToString { it.name }}) found. Extracting and choosing the largest one …" }
                    unarchive(temp)
                }
            }

            val file = (temp.listDirectoryEntriesRecursively()
                .filter { imgFilter.invoke(it) }
                .maxByOrNull { it.getSize() }
                ?: throw IllegalStateException("An unknown error occurred while unarchiving. $temp was supposed to contain the unarchived file but was empty."))
            logLine { "Extracted ${file.getSize()}" }
            file
        }
    }

    private fun Path.filterArchiveEntries() = listArchive().also {
        require(it.isNotEmpty()) { "Cannot extract image from archive $this as it's empty." }
    }.let { list -> list.filter { entry -> imgFilter(Path.of(entry.name)) } }
}
