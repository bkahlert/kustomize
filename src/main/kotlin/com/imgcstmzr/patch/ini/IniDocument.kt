package com.imgcstmzr.patch.ini

import com.imgcstmzr.util.readAll
import java.nio.file.Path

@Suppress("PublicApiImplicitType")
class IniDocument private constructor(mutableElements: Collection<Line>) : Collection<IniDocument.Line> by mutableElements {
    private val lines: MutableList<Line> = mutableElements.toMutableList()

    constructor(input: String) : this(input.lines().map { Line(it) }.toList())
    constructor(path: Path) : this(path.readAll())

    override fun toString(): String = lines.joinToString("\n")

    fun findKey(name: String, sectionName: String? = null): List<KeyLine> {
        val searchScope: IntRange = sectionName?.let { findSection(it) ?: return@findKey emptyList() } ?: 0..lines.size
        val toSearchIn = lines.subList(searchScope.first, searchScope.last)
        return toSearchIn.filter { line -> line.key?.let { it.name == name } == true }.map { it.key!! }
    }

    fun findSection(sectionName: String?): IntRange? =
        findSectionStartIndex(sectionName)?.let { start ->
            findSectionStartIndex(null, start + 1)?.let { end -> start..end } ?: start..lines.size
        }

    /**
     * Returns the index of the line that represents the start of the [SectionLine] with the given [sectionName].
     *
     * If [sectionName] is omitted any [SectionLine] matches. Since the last matching section is returned if multiple exists, omitting always returns the last section.
     * The search starts at line with index [offset].
     */
    private fun findSectionStartIndex(sectionName: String?, offset: Int = 0): Int? =
        lines.mapIndexed { index: Int, line: Line ->
            if (index < offset) return@mapIndexed null
            val section = line.section ?: return@mapIndexed null
            if (sectionName == null) return@mapIndexed index
            if (sectionName == section.sectionName) return@mapIndexed index
            null
        }.filterNotNull().maxOrNull()

    fun createKeyIfMissing(name: String, value: String, sectionName: String): List<KeyLine> {
        val keys = findKey(name, sectionName)
        val matchedKeys = keys.filter { key -> key.values.map { it.trim() }.contains(value) }
        if (matchedKeys.isNotEmpty()) return matchedKeys

        if (keys.isNotEmpty()) return keys.onEach { it.values += value }

        if (findSection(sectionName) == null) append("[$sectionName]")
        append("$name=$value")
        return findKey(name, sectionName)
    }

    fun append(it: String): Line {
        val element = Line(it)
        lines.add(element)
        return element
    }

    fun save(path: Path) {
        path.toFile().writeText(toString())
    }

    class SectionLine(input: String) : RegexElement(input, false) {
        var sectionIndent by regex("^\\s*")
        var sectionNameLeftBracket by regex("[\\[]")
        var sectionName by regex("[^]]*")
        var sectionNameRightBracket by regex("[]]")
        var sectionTrailingRubbish by regex("\\s*")
    }

    class KeyLine(input: String) : RegexElement(input, false) {
        var margin by regex("^\\s*")
        var name by regex("[\\w]+")
        var leftPadding by regex("\\s*")
        var assignmentSymbol by regex("[:=]")
        var rightPadding by regex("\\s*")
        private var value by regex("[^#]*[\\w$]")

        var values: List<String>
            get() = value.split(",").toMutableList()
            set(list) {
                value = list.joinToString(",")
            }
    }

    class CommentLine(input: String) : RegexElement(input, false) {
        var outerPadding by regex("\\s*")
        var delimiter by regex("[;#]")
        var innerPadding by regex("\\s*")
        var commentText by regex(".*$")
    }

    class Line(input: String) : CompositeRegexElement(input) {
        val section by optionalElement(::SectionLine)
        val key by optionalElement(::KeyLine)
        val comment by optionalElement(::CommentLine)
    }
}
