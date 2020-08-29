package com.imgcstmzr.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.sources.ExperimentalValueSourceApi
import com.github.ajalt.clikt.sources.ValueSource
import com.github.ajalt.clikt.sources.ValueSource.Invocation
import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import io.github.config4k.extract
import java.io.File
import java.net.URL

/**
 * Makes all types of configurations available by adapting [config4k](https://www.kotlinresources.com/library/config4k/)'s [config].
 *
 * **Important:** The first config level is treated as a config name—similar to documents in YAML.
 */
@OptIn(ExperimentalValueSourceApi::class)
class Config4kValueSource(private val name: String, private val config: Config) : ValueSource {
    private val prefix = "$name.$name."
    private val nameKey = "$name.$name.name"

    private fun computeKey(context: Context, option: Option): String {
        val relativeKey = context.commandNameWithParents().drop(1) + ValueSource.name(option)
        return prefix + if (relativeKey.last() == "name") "name" else relativeKey.joinToString(".")
    }

    override fun getValues(context: Context, option: Option): List<Invocation> {
        val key = computeKey(context, option)
        if (key == nameKey) return Invocation.just(name)
        val value = getValue(key)
        if (value.isEmpty() && context.parent != null) {
            return getValues(context.parent!!, option)
        }
        return value
    }

    private fun getValue(key: String): List<Invocation> {
        if (!config.hasPath(key)) {
            return emptyList()
        }
        return try {
            config.getString(key)?.let { Invocation.just(it) }.orEmpty()
        } catch (e: Exception) {
            try {
                config.getStringList(key)?.let { Invocation.just(it) }.orEmpty()
            } catch (e: Exception) {
                val stringMap: List<List<String>> = config.extract(key)
                return stringMap.map { Invocation(listOf("${it[0]}=${it[1]}")) }
            }
        }
    }

    companion object {

        fun from(serializedConfig: String): Config4kValueSource {
            val parseConfig: Config = ConfigFactory.parseString(serializedConfig)
            val names = parseConfig.entrySet().map { it.key.split('.')[0] }.distinct()
            check(names.size == 1) { "Config must contain exactly one name giving top element, but ${names.size} found: $names" }
            return Config4kValueSource(names[0], parseConfig.atKey(names[0]))
        }

        fun ValueSource?.update(configFile: File) {
            (this as Proxy).delegate = from(configFile.readText())
        }

        fun ValueSource?.update(configUrl: URL) {
            (this as Proxy).delegate = from(configUrl.readText())
        }


        fun proxy(): ValueSource = Proxy()
    }

    @OptIn(ExperimentalValueSourceApi::class)
    private class Proxy() : ValueSource {
        var delegate: ValueSource? = null

        override fun getValues(context: Context, option: Option): List<ValueSource.Invocation> =
            delegate?.getValues(context, option) ?: emptyList()
    }

}