package com.imgcstmzr

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.sources.ExperimentalValueSourceApi
import com.github.ajalt.clikt.sources.ValueSource
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory

/**
 * Makes all types of configurations available by adapting [config4k](https://www.kotlinresources.com/library/config4k/)'s [config].
 *
 * **Important:** The first config level is treated as a config name—similar to documents in YAML.
 */
@OptIn(ExperimentalValueSourceApi::class)
class ConfigValueSource(val name: String, val config: Config) : ValueSource {
    val prefix = "$name.$name."
    val nameKey = "$name.$name.name"

    fun computeKey(context: Context, option: Option): String {
        val relativeKey = context.commandNameWithParents().drop(1) + ValueSource.name(option)
        return prefix + if (relativeKey.last() == "name") "name" else relativeKey.joinToString(".")
    }

    override fun getValues(context: Context, option: Option): List<ValueSource.Invocation> {
        val key = computeKey(context, option)
        if (key == nameKey) return ValueSource.Invocation.just(name)
        if (!config.hasPath(key)) return emptyList()
        return config.getString(key)?.let { ValueSource.Invocation.just(it) }.orEmpty()
    }

    companion object {
        fun parse(value: String): ConfigValueSource {
            val parseConfig: Config = ConfigFactory.parseString(value)
            val names = parseConfig.entrySet().map { it.key.split('.')[0] }.distinct()
            check(names.size == 1) { "Config must contain exactly one name giving top element, but ${names.size} found: $names" }
            return ConfigValueSource(names[0], parseConfig.atKey(names[0]))
        }
    }
}
