package com.bkahlert.koodies.docker

import com.bkahlert.koodies.regex.RegexBuilder
import com.bkahlert.koodies.string.random
import com.imgcstmzr.process.Exec
import com.imgcstmzr.process.Exec.Sync.checkIfOutputContains

object Docker {
    val isRunning get() = !checkIfOutputContains("docker info", "error")
}

val containerNameRegex: Regex = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{7,}")

fun String.toContainerName(): String {
    var replaceWithXToGuaranteeAValidName = true
    return map { c ->
        val isAlphaNumeric = RegexBuilder.alphanumericCharacters.contains(c)
        if (isAlphaNumeric) replaceWithXToGuaranteeAValidName = false
        if (replaceWithXToGuaranteeAValidName) return@map "X"
        when {
            isAlphaNumeric -> c
            "._-".contains(c) -> c
            c.isWhitespace() -> "-"
            else -> '_'
        }
    }.joinToString("",
        postfix = (8 - length)
            .takeIf { it > 0 }?.let {
                String.random(it, String.random.alphanumericCharacters)
            } ?: "")
}
