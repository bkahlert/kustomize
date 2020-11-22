package com.bkahlert.koodies.nio.file

import java.nio.file.Path

/**
 * Contains this path in its absolute and normalized form
 * (e.g. `some/./where/file.txt` ➜ `/volume/some/where/file.txt`).
 *
 * *Hint:* This implementation—as it should be—does not rely on [toString] so
 * you're free to override [toString] and/or extend [Path].
 */
@Deprecated("use last() instead", replaceWith = ReplaceWith("serialized"))
val Path.absolutePathString: String
    get() = serialized
