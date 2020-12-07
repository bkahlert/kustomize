@file:Suppress("PackageDirectoryMismatch")

package koodies.io.path

import java.io.BufferedWriter
import java.nio.file.OpenOption
import java.nio.file.Path
import kotlin.io.path.bufferedWriter

fun Path.bufferedWriter(vararg options: OpenOption): BufferedWriter = bufferedWriter(Charsets.UTF_8, DEFAULT_BUFFER_SIZE, *options)

