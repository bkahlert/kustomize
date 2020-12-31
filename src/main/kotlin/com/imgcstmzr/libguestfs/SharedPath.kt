package com.imgcstmzr.libguestfs

import com.imgcstmzr.runtime.OperatingSystemImage
import koodies.io.path.asString
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

private const val DIRNAME = "shared"

enum class SharedPath(val resolveRoot: (osImage: OperatingSystemImage) -> Path) {
    Disk({ Path.of("/") }),
    Docker({ Path.of("/$DIRNAME") }),
    Host({
        it.file.resolveSibling(DIRNAME).apply { if (!exists()) createDirectories() }
    });

    fun OperatingSystemImage.resolve(imagePath: Path): Path = resolve(imagePath.asString())
    fun OperatingSystemImage.resolve(imagePath: String): Path = resolveRoot(this).resolve(imagePath.removePrefix("/").removePrefix("$DIRNAME/"))
}
