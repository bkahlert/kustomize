package com.bkahlert.koodies.io

import com.bkahlert.koodies.io.Archiver.addToArchive
import com.bkahlert.koodies.io.Archiver.unarchiveTo
import com.bkahlert.koodies.nio.bufferedInputStream
import com.bkahlert.koodies.nio.bufferedOutputStream
import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.requireEmpty
import com.bkahlert.koodies.nio.file.requireExists
import com.bkahlert.koodies.nio.file.requireExistsNot
import com.imgcstmzr.util.addExtension
import com.imgcstmzr.util.delete
import com.imgcstmzr.util.mkdirs
import com.imgcstmzr.util.removeExtension
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.nio.file.Path


/**
 * Provides (un-)archiving functionality for the TAR archive format.
 */
object TarArchiver {
    /**
     * Archives this directory using the TAR archive format.
     *
     * By default the existing directory name is used and `.tar` appended.
     */
    fun Path.tar(
        destination: Path = addExtension("tar"),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.delete(true) else destination.requireExistsNot()
        TarArchiveOutputStream(destination.bufferedOutputStream()).use { addToArchive(it) }
        return destination
    }

    /**
     * Unarchives this TAR archive.
     *
     * By default the existing file name is used with the `.tar` suffix removed.
     */
    fun Path.untar(
        destination: Path = removeExtension("tar"),
        overwrite: Boolean = false,
    ): Path {
        requireExists()
        if (overwrite) destination.delete(true)
        if (!destination.exists) destination.mkdirs()
        if (!overwrite) destination.requireEmpty()
        TarArchiveInputStream(bufferedInputStream()).use { it.unarchiveTo(destination) }
        return destination
    }
}
