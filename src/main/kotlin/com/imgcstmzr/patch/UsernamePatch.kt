package com.imgcstmzr.patch

import com.github.ajalt.clikt.output.TermUi
import com.imgcstmzr.util.debug
import com.imgcstmzr.util.readAll
import com.imgcstmzr.util.readAllLines
import com.imgcstmzr.util.readable
import com.imgcstmzr.util.writeText
import java.nio.file.Path

// "Change username pi to $username?"
class UsernamePatch(private val oldUsername: String, private val newUsername: String) : Patch {

    override val name: String = "Username Change"

    override val actions: List<Action<*>>
        get() =
            listOf(
                "/etc/passwd", "/etc/group",
                "/etc/shadow", "/etc/gshadow",
                "/etc/subuid", "/etc/subgid",
            ).map {
                PathAction(Path.of(it), requireNotMatchingContent(oldUsernamePattern)) { path ->
                    path.readAll()
                        .replace(oldUsernamePattern) { newUsername }
                        .also { patchedContent -> path.writeText(patchedContent) }
                        .also { patchedContent -> TermUi.debug("Replaced: ($oldUsernamePattern) -> $newUsername\n$patchedContent") }
                }
            }

    private val oldUsernamePattern = Regex("\\b$oldUsername\\b", RegexOption.MULTILINE)

    private fun requireNotMatchingContent(regex: Regex): (Path) -> Unit {
        return { path ->
            require(path.readable) { "$path can't be read" }
            val readAllLines = path.readAllLines()
            require(readAllLines.none {
                (oldUsernamePattern.containsMatchIn(it)).also { result ->
                    TermUi.debug(oldUsernamePattern.pattern + " containsMatchIn " + it + " => " + result)
                }
            }) { "$path matches $regex" }
        }
    }
}
