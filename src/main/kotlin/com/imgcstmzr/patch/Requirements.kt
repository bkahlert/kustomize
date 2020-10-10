package com.imgcstmzr.patch

import com.github.ajalt.clikt.output.TermUi
import com.imgcstmzr.cli.KeyValueDocument
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.isReadable
import com.imgcstmzr.util.readAllLines
import java.nio.file.Path

object Requirements {

    fun requireContainingContent(string: String): (Path) -> Unit {
        return {
            require(it.toFile().canRead()) { "$it can't be read" }
            require(it.toFile().readLines().any { line -> line.contains(string) }) { "$it does not contain $string " }
        }
    }

    fun requireContainingKeyValue(key: String, vararg values: String): (Path) -> Unit {
        return { it: Path ->
            require(it.toFile().canRead()) { "$it can't be read" }
            require(it.toFile().canRead() && values.all { value ->
                KeyValueDocument(it).containsValue(key,
                    value)
            }) { "$it does not contain ${values.toList()} " }
        }
    }

    fun requireNotMatchingContent(regex: Regex, lazyMessage: () -> Any): (Path) -> Unit {
        return { path ->
            require(path.isReadable) { "$path can't be read" }
            require(path.readAllLines().none {
                (regex.containsMatchIn(it)).also { result ->
                    TermUi.debug(regex.pattern + " containsMatchIn " + it + " => " + result)
                }
            }, lazyMessage)
        }
    }
}
