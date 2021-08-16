package com.bkahlert.kustomize.libguestfs

import com.bkahlert.kommons.io.compress.Archiver.listArchive
import com.bkahlert.kommons.io.compress.Archiver.unarchive
import com.bkahlert.kommons.io.path.extensionOrNull
import com.bkahlert.kommons.io.path.getSize
import com.bkahlert.kommons.io.path.listDirectoryEntriesRecursively
import com.bkahlert.kommons.io.path.moveToDirectory
import com.bkahlert.kommons.io.path.selfCleaning
import com.bkahlert.kommons.io.path.tempDir
import com.bkahlert.kommons.io.path.uriString
import com.bkahlert.kommons.time.hours
import com.bkahlert.kommons.tracing.runSpanning
import java.nio.file.Path

object ImageExtractor {

    private val temp by com.bkahlert.kustomize.Kustomize.temp.resolve("image-extract").selfCleaning(1.hours, 5)

    val imgFilter: (Path) -> Boolean = { path ->
        path.extensionOrNull.equals("img", ignoreCase = true)
            && !path.startsWith("__MACOSX")
            && !path.last().toString().startsWith("._")
    }

    fun Path.extractImage(): Path = runSpanning("Unarchiving $uriString (${getSize()})") {
        if (imgFilter(this@extractImage)) {
            log("$fileName is already an image.")
            this@extractImage
        } else {
            val temp = temp.tempDir("extract-")

            val filteredArchiveEntries = filterArchiveEntries()
            when (filteredArchiveEntries.size) {
                0 -> {
                    log("No image found. Using archive as image.")
                    moveToDirectory(temp)
                }
                1 -> {
                    runSpanning("Extracting found image ${filteredArchiveEntries.first().name}") {
                        unarchive(temp)
                    }
                }
                else -> {
                    runSpanning("Multiple image candidates (${filteredArchiveEntries.joinToString { it.name }}) found. Extracting largest candidate") {
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
