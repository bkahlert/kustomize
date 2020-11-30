package com.imgcstmzr.patch.ini

import com.bkahlert.koodies.string.quoted
import com.imgcstmzr.patch.ini.ParsedElement.Companion.toParsedElement
import com.imgcstmzr.patch.ini.ParsedElements.Companion.toParsedElements
import com.imgcstmzr.util.namedGroup
import com.imgcstmzr.util.values

open class RegexParser(groups: Iterable<Pair<String, String>>, matchCompleteLine: Boolean = false) {
    constructor(vararg groups: Pair<String, String>, matchCompleteLine: Boolean = false) : this(groups.asIterable(), matchCompleteLine)

    private val names: List<String> = groups.map { (name, _) -> name }
    val regex: Regex = Regex(groups.joinToString(
        separator = "",
        prefix = if (matchCompleteLine) "^" else "",
        postfix = if (matchCompleteLine) "$" else "") { (name, pattern) ->
        Regex.namedGroup(name, pattern)
    })
    private val groupRegexes: Map<String, Regex> = groups.map { (name, pattern) -> name to Regex(Regex.namedGroup(name, pattern)) }.toMap()

    private fun parseAsSequence(input: CharSequence): Sequence<ParsedElement> =
        regex.findAll(input).map { it.values(names).map { (k, v) -> k to v!! }.toParsedElement() }

    fun matches(input: CharSequence): Boolean = parseAsSequence(input).toList().isNotEmpty()

    fun parseSingle(input: CharSequence): ParsedElement {
        val iterator = parseAsSequence(input).iterator()
        require(iterator.hasNext()) { "${input.quoted} does not match ${regex.pattern.quoted}." }
        val single = iterator.next()
        require(!iterator.hasNext()) { "Sequence has more than one element ($single), e.g. ${iterator.next()}" }
        return single
    }

    fun parseAll(input: CharSequence): ParsedElements = parseAsSequence(input).toParsedElements()

    fun replace(parsed: ParsedElement, vararg replacements: Pair<String, String>): ParsedElement {
        val transformationMap = replacements
            .map { (name, replacement) -> name to { oldValue: String -> replacement } }
            .map { (name, transformation) -> name to verifyingTransformation(name, transformation) }
            .toMap()
        return parsed.transform(transformationMap)
    }

    fun replaceAll(input: CharSequence, vararg replacements: Pair<String, String>): ParsedElements {
        val transformationMap = replacements
            .map { (name, replacement) -> name to { oldValue: String -> replacement } }
            .map { (name, transformation) -> name to verifyingTransformation(name, transformation) }
            .toMap()
        return parseAsSequence(input).map { parsedElement -> parsedElement.transform(transformationMap) }.toParsedElements()
    }

    fun transform(parsed: ParsedElement, vararg transformations: Pair<String, (String) -> String>): ParsedElement {
        val transformationMap = transformations
            .map { (name, transformation) -> name to verifyingTransformation(name, transformation) }
            .toMap()
        return parsed.transform(transformationMap)
    }

    fun transformAll(input: CharSequence, vararg transformations: Pair<String, (Int, String) -> String>): ParsedElements =
        parseAsSequence(input).mapIndexed { index, match ->
            val transformationMap = transformations
                .map { (name, transformation) -> name to { oldValue: String -> transformation(index, oldValue) } }
                .map { (name, transformation) -> name to verifyingTransformation(name, transformation) }
                .toMap()
            match.transform(transformationMap)
        }.toParsedElements()

    fun verifyingTransformation(name: String, transformation: (String) -> String): (String) -> String {
        return { input: String ->
            val transformed = transformation(input)
            val regex = groupRegexes[name] ?: throw IllegalStateException("No regex found for ${name.quoted}")
            require(regex.matchEntire(transformed) != null) {
                "The value ${input.quoted} of field ${name.quoted} could not be changed to ${transformed.quoted} as it does not match ${regex.pattern}"
            }
            transformed
        }
    }

    fun ParsedElement.transform(transformations: Map<String, (String) -> String>): ParsedElement =
        entries.map { (key, value) -> key to (transformations[key] ?: { it })(value) }.toParsedElement()

}

open class ParsedElement private constructor(
    protected val parsed: Map<String, String>,
    private val rendered: String,
) : Map<String, String> by parsed, CharSequence by rendered {
    companion object {
        fun of(vararg parsed: Pair<String, String>) = of(parsed.toList())
        fun of(parsed: Iterable<Pair<String, String>>): ParsedElement = ParsedElement(parsed.toMap(), parsed.joinToString("") { (_, value) -> value })
        fun Iterable<Pair<String, String>>.toParsedElement(): ParsedElement = of(this)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParsedElement

        if (parsed != other.parsed) return false

        return true
    }

    override fun hashCode(): Int = parsed.hashCode()

    override fun toString(): String = rendered
}

open class ParsedElements private constructor(
    private val parsed: List<ParsedElement>,
    private val rendered: String,
) : List<ParsedElement> by parsed {
    companion object {
        fun of(vararg parsed: ParsedElement) = of(parsed.toList())
        fun of(parsed: Iterable<ParsedElement>) = ParsedElements(parsed.toList(), parsed.joinToString(""))
        fun Sequence<ParsedElement>.toParsedElements(): ParsedElements = ParsedElements.of(this.toList())
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ParsedElements

        if (parsed != other.parsed) return false

        return true
    }

    override fun hashCode(): Int = parsed.hashCode()

    override fun toString(): String = rendered
}
