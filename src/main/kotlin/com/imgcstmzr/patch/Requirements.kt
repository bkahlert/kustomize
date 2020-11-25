package com.imgcstmzr.patch

import com.bkahlert.koodies.nio.file.readLines
import com.imgcstmzr.cli.KeyValueDocument
import com.imgcstmzr.util.isReadable
import java.nio.file.Path

// TODO refactor to Path extension functions
object Requirements {

    fun requireContainingContent(string: String): (Path) -> Unit = { path: Path ->
        require(path.isReadable) { "$path can't be read" }
        require(path.readLines().any { line -> line.contains(string) }) { "$path does not contain $string " }
    }

    fun requireContainingKeyValue(key: String, vararg values: String): (Path) -> Unit = { path: Path ->
        require(path.isReadable) { "$path can't be read" }
        require(path.isReadable && values.all { value ->
            KeyValueDocument(path).containsValue(key, value)
        }) { "$path does not contain ${values.toList()} " }
    }

    fun requireNotMatchingContent(regex: Regex, lazyMessage: () -> Any): (Path) -> Unit = { path ->
        require(path.isReadable) { "$path can't be read" }
        require(path.readLines().none {
            regex.containsMatchIn(it)
        }, lazyMessage)
    }
}
