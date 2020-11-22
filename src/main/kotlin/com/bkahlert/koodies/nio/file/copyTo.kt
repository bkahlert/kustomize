package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.nio.exception.directoryNotEmpty
import com.bkahlert.koodies.nio.exception.fileSystemException
import com.bkahlert.koodies.nio.exception.noSuchFile
import com.bkahlert.koodies.unit.Size.Companion.size
import com.imgcstmzr.util.isDirectory
import com.imgcstmzr.util.isFile
import org.apache.commons.io.IOUtils
import java.io.File
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Copies this path to the [target] recursively exactly as [File.copyRecursively] would do it.
 * (In short: A file `/src/file` or a directory `/src/dir` copied to `/dest/target` would then be located at `/dest/target`)
 *
 * Yet, there are two differences:
 * 1. [Java's non-blocking I/O](https://en.wikipedia.org/wiki/Non-blocking_I/O_(Java)) is used for this purpose which
 *    extends the use case too all implementations of [FileSystem]
 * 2. File attributes can be copied as well using the [preserve] flag *(default: off)*.
 *    (Since files are copied top-down, the [lastModified] attribute might be—although preserved—again be updated.
 *
 * Should `target` already exist and be a directory, an exception is thrown to avoid serious data loss.
 *
 * @see copyToDirectory
 */
fun Path.copyTo(
    target: Path,
    overwrite: Boolean = false,
    preserve: Boolean = false,
    onError: (Path, FileSystemException) -> OnErrorAction = { _, exception -> throw exception },
): Path {
    if (notExists) {
        onError(this, noSuchFile(this, target, "Source file doesn't exist.")) != OnErrorAction.TERMINATE
        return target
    }

    try {
        for (src in walkTopDown().onFail { f, e -> if (onError(f, e) == OnErrorAction.TERMINATE) throw TerminateException(f) }) {
            if (src.notExists) {
                if (onError(src, noSuchFile(src, "Source file doesn't exist.")) == OnErrorAction.TERMINATE) return target
            } else {
                val dstFile = target.resolveBetweenFileSystems(relativize(src))
                if (dstFile.exists) {
                    if (!src.isDirectory || !dstFile.isDirectory) {
                        if (src.isFile && dstFile.isFile && src.size == dstFile.size) {
                            if (IOUtils.contentEquals(src.inputStream(), dstFile.inputStream())) continue
                        }
                        val stillExists = if (overwrite) dstFile.delete(true).exists else true

                        if (stillExists) {
                            if (onError(dstFile,
                                    fileAlreadyExists(src, dstFile, "The destination file already exists.")) == OnErrorAction.TERMINATE
                            ) return target
                            continue
                        }
                    }
                }

                if (dstFile.parent.notExists) dstFile.parent.mkdirs()

                if (src.isDirectory) {
                    if (dstFile.notExists) {

                        val createdDir = Files.copy(src, dstFile, *CopyOptions.enumArrayOf(replaceExisting = overwrite, copyAttributes = preserve))
                        val isEmpty = createdDir.run { isDirectory && isEmpty }
                        if (!isEmpty) {
                            if (onError(src, directoryNotEmpty(dstFile)) == OnErrorAction.TERMINATE) return target
                        }
                    }
                } else {
                    val copiedFile = Files.copy(src, dstFile, *CopyOptions.enumArrayOf(replaceExisting = overwrite, copyAttributes = preserve))
                    if (copiedFile.size != src.size) {
                        if (onError(src,
                                fileSystemException(src, dstFile, "Only ${copiedFile.size} out of ${src.size} were copied.")) == OnErrorAction.TERMINATE
                        )
                            return target
                    }
                }
            }
        }
        return target
    } catch (e: TerminateException) {
        return target
    }
}

class TerminateException(path: Path) : FileSystemException(path.toString())
