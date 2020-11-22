package com.bkahlert.koodies.nio.file

import com.imgcstmzr.util.isDirectory
import java.io.File
import java.nio.file.FileAlreadyExistsException
import java.nio.file.FileSystemException
import java.nio.file.NoSuchFileException
import java.nio.file.Path

/**
 * Copies this path to the [targetDirectory] recursively exactly as [File.copyRecursively] would do it,
 * if the call happened on the target's parent (directory).
 * (In short: A file `/src/file` or a directory `/src/dir` copied to `/dest/target`
 *  would then be located at `/dest/target/file` respectively `/dest/target/dir`)
 *
 * Yet, there are two differences:
 * 1. [Java's non-blocking I/O](https://en.wikipedia.org/wiki/Non-blocking_I/O_(Java)) is used for this purpose which
 *    extends the use case too all implementations of [FileSystem]
 * 2. File attributes can be copied as well using the [preserve] flag *(default: off)*.
 *    (Since files are copied top-down, the [lastModified] attribute might be—although preserved—again be updated.
 *
 * Should `targetDirectory` already exist and contain a directory with the same name as this path,
 * an exception is thrown to avoid serious data loss.
 *
 * @see copyTo
 */
fun Path.copyToDirectory(
    targetDirectory: Path,
    overwrite: Boolean = false,
    preserve: Boolean = false,
    onError: (Path, FileSystemException) -> OnErrorAction = { _, exception -> throw exception },
): Path {
    if (notExists) {
        onError(this, NoSuchFileException(this.toString(), "$targetDirectory", "The source file doesn't exist.")) != OnErrorAction.TERMINATE
        return targetDirectory
    }

    if (!targetDirectory.exists) targetDirectory.mkdirs()
    if (!targetDirectory.isDirectory) {
        onError(this,
            FileAlreadyExistsException(this.toString(), "$targetDirectory", "The destination must not exist or be a directory.")) != OnErrorAction.TERMINATE
        return targetDirectory
    }
    
    return copyTo(targetDirectory.resolveBetweenFileSystems(fileName), overwrite = overwrite, preserve = preserve, onError = onError)
}
