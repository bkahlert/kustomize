package com.imgcstmzr.util

/**
 * Returns a pattern that optionally matches this [Regex].
 *
 * Example: `abc` becomes `(?:abc)?`
 */
fun Regex.optional(): String = "(?:$pattern)?"

/**
 * Returns a group with a [name] matching [pattern].
 *
 * Once matches the matching group can be retrieved using [MatchNamedGroupCollection] starting with Kotlin 1.1.
 * Otherwise [MatchResult.values] can be used.
 */
fun Regex.Companion.namedGroup(name: String, pattern: String): String = "(?<$name>$pattern)"

/**
 * Returns all values that matched the groups with corresponding [groupNames].
 *
 * E.g. if `(?<a>...)(?<b>...)` matched, `result.values(listOf("b"))` returns a map with the [Map.Entry] `b` and its matched value.
 *
 * Important: In Kotlin 1.0 group names are not supported but in order to still work [groupNames] is expected to contain all used group names in the order as their appear in the regular expression (e.g. `listOf("a", "b")`).
 */
fun MatchResult.values(groupNames: List<String>): Map<String, String?> {
    return if (groups is MatchNamedGroupCollection) {
        groupNames.mapIndexed { index, name -> name to groups[name]?.value }.toMap()
    } else {
        groupNames.mapIndexed { index, name -> name to groups[index + 1]?.value }.toMap()
    }
}

/**
 * Provides access to [MatchResult.groups] as [MatchNamedGroupCollection] in order to access a [MatchGroup] by its name.
 */
@SinceKotlin("1.1")
val MatchResult.namedGroups: MatchNamedGroupCollection
    get() = groups as MatchNamedGroupCollection

/**
 * Returns the matched [MatchGroup] by its [name].
 */
@SinceKotlin("1.1")
fun MatchResult.group(name: String): MatchGroup? = namedGroups[name]

/**
 * Returns the matched [MatchGroup] by its [index].
 */
fun MatchResult.group(index: Int): MatchGroup? = group(index)

/**
 * Returns the value of the matched [MatchGroup] with the provided [name].
 */
@SinceKotlin("1.1")
fun MatchResult.groupValue(name: String): String? = group(name)?.value

/**
 * Returns the value of the matched [MatchGroup] with the provided [index].
 */
fun MatchResult.groupValue(index: Int): String? = group(index)?.value

/**
 * Returns the value of the [MatchGroup] with the provided [name].
 */
@SinceKotlin("1.1")
operator fun MatchResult.get(name: String): String? = groupValue(name)

/**
 * Returns the value of the matched [MatchGroup] with the provided [index].
 */
operator fun MatchResult.get(index: Int): String? = groupValue(index)
