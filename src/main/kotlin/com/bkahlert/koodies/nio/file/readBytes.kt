package com.bkahlert.koodies.nio.file

import java.nio.file.Files
import java.nio.file.Path

/**
 * Reads all the bytes from a file up to 2GB.
 * The method ensures that the file is closed when all bytes have been read or
 * an I/O error, or other runtime exception, is thrown.
 *
 * @see Files.readAllBytes
 */
fun Path.readBytes(): ByteArray = Files.readAllBytes(this)
