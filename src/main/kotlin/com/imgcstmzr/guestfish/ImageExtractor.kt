package com.imgcstmzr.guestfish

import com.bkahlert.koodies.io.Archiver.listArchive
import com.bkahlert.koodies.io.Archiver.unarchive
import com.bkahlert.koodies.nio.file.extensionOrNull
import com.bkahlert.koodies.nio.file.listRecursively
import com.bkahlert.koodies.unit.Size.Companion.size
import com.github.ajalt.clikt.output.TermUi.echo
import java.nio.file.Path

object ImageExtractor {
    val imgFilter: (Path) -> Boolean = { path ->
        path.extensionOrNull.equals("img", ignoreCase = true)
            && !path.startsWith("__MACOSX")
            && !path.last().toString().startsWith("._")
    }

    fun Path.extractImage(imageBuilder: (Path) -> Path): Path {
        if (imgFilter(this)) {
            echo("No need for extraction as $fileName seems to already be an image.")
            return this
        }

        val temp = com.bkahlert.koodies.nio.file.tempDir("imgcstmzr-")
        echo("Unarchiving $fileName ($size)...", trailingNewline = false)

        val filteredArchiveEntries = filterArchiveEntries()
        when (filteredArchiveEntries.size) {
            0 -> {
                echo("No image found. Considering the archive the image itself.")
                return imageBuilder(this)
            }
            1 -> {
                echo("Image ${filteredArchiveEntries.first().name} found. Extracting.")
                unarchive(temp)
            }
            else -> {
                echo("Multiple image candidates (${filteredArchiveEntries.joinToString { it.name }}) found. Extracting and choosing the largest one.")
                unarchive(temp)
            }
        }

        val file = (temp.listRecursively()
            .filter { imgFilter.invoke(it) }
            .maxByOrNull { it.size }
            ?: throw IllegalStateException("An unknown error occurred while unarchiving. $temp was supposed to contain the unarchived file but was empty."))
        return file.also { echo(" Completed (${it.size}).") }
    }

    private fun Path.filterArchiveEntries() = listArchive().also {
        require(it.isNotEmpty()) { "Cannot extract image from archive $this as it's empty." }
    }.let { list -> list.filter { entry -> imgFilter(Path.of(entry.name)) } }
}
