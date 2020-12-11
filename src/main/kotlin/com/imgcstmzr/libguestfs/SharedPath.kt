package com.imgcstmzr.libguestfs

import com.bkahlert.koodies.nio.file.exists
import com.bkahlert.koodies.nio.file.mkdirs
import com.bkahlert.koodies.nio.file.serialized
import com.imgcstmzr.runtime.OperatingSystemImage
import java.nio.file.Path

private const val DIRNAME = "shared"

enum class SharedPath(val resolveRoot: (osImage: OperatingSystemImage) -> Path) {
    Disk({ Path.of("/") }),
    Docker({ Path.of("/$DIRNAME") }),
    Host({
        it.file.resolveSibling(DIRNAME).apply { if (!exists) mkdirs() }
    });

    fun OperatingSystemImage.resolve(imagePath: Path): Path = resolve(imagePath.serialized)
    fun OperatingSystemImage.resolve(imagePath: String): Path = resolveRoot(this).resolve(imagePath.removePrefix("/"))
}
