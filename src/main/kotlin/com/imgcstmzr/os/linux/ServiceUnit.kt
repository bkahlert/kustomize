package com.imgcstmzr.os.linux

import com.imgcstmzr.os.LinuxRoot
import koodies.text.LineSeparators.lines
import org.intellij.lang.annotations.Language

@Suppress("DataClassPrivateConstructor")
data class ServiceUnit private constructor(
    val name: String,
    val text: CharSequence,
) {
    constructor(name: String, @Language("Unit File (systemd)") text: String) : this(name, text.trimIndent() as CharSequence)

    val diskFile = LinuxRoot.etc.systemd.system / name

    /**
     * Returns the content of the section with the specified [sectionName] if it exists.
     */
    operator fun get(sectionName: String): String? {
        val section = text.split("[$sectionName]")
        return when (section.size) {
            1 -> null
            2 -> section[1].split("\\[.*?]".toRegex())[0].trim()
            else -> error("Error parsing $text")
        }
    }

    /**
     * Contains the dependencies specified in the `Install` section using `WantedBy`.
     */
    val wantedBy: List<String>
        get() = get("Install")?.run {
            lines().filter { it.startsWith("WantedBy") }.map { it.replace("WantedBy\\s*=\\s*".toRegex(), "") }
        } ?: emptyList()
}
