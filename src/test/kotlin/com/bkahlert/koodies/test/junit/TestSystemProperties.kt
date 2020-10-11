package com.bkahlert.koodies.test.junit

import com.bkahlert.koodies.test.junit.SystemProperty.Companion.flag
import com.bkahlert.koodies.test.junit.TestSystemProperties.skipUnitTests
import kotlin.properties.ReadOnlyProperty

/**
 * Contains [ReadOnlyProperty] that provide access to test relevant system properties.
 *
 * @sample TestSystemProperties.skipUnitTests
 * @sample TestSystemProperties.skipIntegrationTests
 * @sample TestSystemProperties.skipE2ETests
 */
object TestSystemProperties {

    /**
     * The [skipUnitTests] flag evaluates to `false` by default.
     *
     * Activate by using `-DskipUnitTests` or `-DskipUnitTests=true` (resp. `-DskipUnitTests=false`).
     */
    @Suppress("SpellCheckingInspection")
    val skipUnitTests by flag(defaultValue = false)

    /**
     * The [skipIntegrationTests] flag evaluates to `false` by default.
     *
     * Activate by using `-DskipIntegrationTests` or `-DskipIntegrationTests=true` (resp. `-DskipIntegrationTests=false`).
     */
    @Suppress("SpellCheckingInspection")
    val skipIntegrationTests by flag(defaultValue = false)

    /**
     * The [skipE2ETests] flag evaluates to `false` by default.
     *
     * Activate by using `-DskipUnitTests` or `-DskipUnitTests=true` (resp. `-DskipUnitTests=false`).
     */
    @Suppress("SpellCheckingInspection")
    val skipE2ETests by flag(defaultValue = false)

    override fun toString(): String = listOf(
        ::skipUnitTests,
        ::skipIntegrationTests,
        ::skipE2ETests
    ).joinToString { it.name + "=" + it.get() }
}
