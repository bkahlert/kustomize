package com.bkahlert.koodies.docker

import com.bkahlert.koodies.nio.file.serialized
import com.bkahlert.koodies.regex.RegexBuilder
import com.bkahlert.koodies.string.random
import java.nio.file.Path


inline class DockerContainerName(val name: String) {
    val sanitized: String get() = name.sanitize()

    override fun toString(): String = sanitized

    companion object {
        /**
         * A [Regex] that matches valid Docker container names.
         */
        private val regex: Regex = Regex("[a-zA-Z0-9][a-zA-Z0-9._-]{7,}")

        private fun isValid(name: String) = name.matches(regex)

        /**
         * Checks if this [String] is a valid [DockerContainerName]
         * and if not transforms it.
         */
        private fun String.sanitize(): String {
            if (isValid(this)) return this
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
                .also { check(isValid(it)) }
        }

        /**
         * Transforms this [String] to a valid [DockerContainerName].
         */
        fun String.toContainerName(): DockerContainerName = DockerContainerName(sanitize())

        /**
         * Transforms this [Path] to a unique [DockerContainerName].
         */
        fun Path.toUniqueContainerName(): DockerContainerName =
            DockerContainerName(fileName.serialized.take(18) + "-${String.random(4)}")
    }
}
