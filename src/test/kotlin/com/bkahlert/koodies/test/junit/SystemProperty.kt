package com.bkahlert.koodies.test.junit

import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/**
 * Provides read access to system properties using delegates.
 *
 * **Usage**
 * ```
 * // gives access to system property "myProp"
 * // and "default value" in case it does not exist
 * val myProp by option("default value")
 * ```
 */
class SystemProperty<out T>(private val deserialize: String?.() -> T) : ReadOnlyProperty<Any, T> {
    companion object {
        /**
         * Creates a [ReadOnlyProperty] that provides access to a boolean [SystemProperty].
         *
         * @sample TestSystemProperties.skipUnitTests
         * @sample TestSystemProperties.skipIntegrationTests
         * @sample TestSystemProperties.skipE2ETests
         */
        fun flag(defaultValue: Boolean, vararg exceptions: String = arrayOf("", "${!defaultValue}")): SystemProperty<Boolean> =
            SystemProperty { if (this == null) defaultValue else exceptions.contains(this) }

        /**
         * Creates a [ReadOnlyProperty] that provides access to a string based [SystemProperty].
         */
        fun option(absentValue: String): SystemProperty<String> = SystemProperty { this ?: absentValue }
    }

    override operator fun getValue(thisRef: Any, property: KProperty<*>): T {
        val propertyName = if (thisRef is SkipIfSystemPropertyIsTrueOrEmpty) thisRef.systemPropertyName else property.name
        val propertyValue = System.getProperty(propertyName)
        return deserialize(propertyValue)
    }
}
