package com.imgcstmzr.util

import java.nio.file.Path

/**
 * Constructs a path that takes this path as the root of [subPath].
 *
 * **Example 1: "re-rooting an absolute path"** ➜
 * `"/dir/a".asRootFor("/dir/b")` becomes `"/dir/a/dir/b"`
 *
 * **Example 2: "re-rooting a relative path"** ➜
 * `"/dir/b".asRootFor("dir/b")` becomes `"/dir/a/dir/b"`
 *
 */
fun Path.asRootFor(subPath: Path): Path {
    val nameCount = subPath.nameCount
    return if (nameCount > 0) resolve(subPath.subpath(0, nameCount)) else this
}
