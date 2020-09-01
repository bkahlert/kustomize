package com.imgcstmzr.util

import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.util.Comparator.reverseOrder


/**
 * Returns the file name of a [Path] with a replaced [extension].
 * If no extension is present, it will be added.
 */
fun Path.fileNameWithExtension(extension: String) =
    fileName.toString().split(".").let { if (it.size > 1) it.dropLast(1) else it }.plus(extension).joinToString(".")

fun Path.wrap(value: CharSequence): String = toString().wrap(value)

fun Path.quote(): String = toString().quote()

fun Path.readAllBytes(): ByteArray = if (this is ClassPath) this.readAllBytes() else Files.readAllBytes(this)

fun Path.copy(dest: Path, createDirectories: Boolean = true) =
    if (this is ClassPath) {
        this.copy(dest, createDirectories)
    } else {
        if (createDirectories) Files.createDirectories(dest.parent)
        Files.copy(this, dest)
    }

fun Path.delete(recursively: Boolean = false) {
    if (recursively) {
        Files.walk(this).use { it.sorted(reverseOrder()).map(Path::toFile).forEach(File::delete) }
    } else {
        toFile().delete()
    }
}
