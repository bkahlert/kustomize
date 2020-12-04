@file:Suppress("PropertyName")

package com.bkahlert.koodies.builder

import java.util.EnumSet

/**
 * Builder of an [EnumSet] of any enum type [T].
 *
 * @see EnumSetBuilderSamples.directUse
 * @see EnumSetBuilderSamples.indirectUse
 */
interface EnumSetBuilder<T : Enum<T>> {
    companion object {
        fun <T : Enum<T>> build(init: EnumSetBuilderInit<T>): EnumSet<T> =
            object : EnumSetBuilder<T> {}.init().let { EnumSet.copyOf(it) }
    }

    operator fun T.unaryPlus(): Set<T> = setOf(this)
    operator fun Set<T>.plus(element: T): Set<T> = this.union(setOf(element))
    operator fun Set<T>.plus(elements: Iterable<T>): Set<T> = this.union(elements)
}

/**
 * Type [EnumSetBuilder.build] accepts.
 */
typealias EnumSetBuilderInit<T> = EnumSetBuilder<T>.() -> Set<T>

@Suppress("UNUSED_VARIABLE", "unused")
private object EnumSetBuilderSamples {

    enum class Features {
        FeatureA, FeatureB, FeatureC
    }

    fun directUse() {

        val pair = EnumSetBuilder.build<Features> {
            +Features.FeatureA + Features.FeatureC
        }

    }

    fun indirectUse() {

        fun builderAcceptingFunction(init: EnumSetBuilderInit<Features>) {
            val enumSet = EnumSetBuilder.build(init)
            println("Et voilà, $enumSet")
        }

        val enumSet = builderAcceptingFunction { +Features.FeatureA + Features.FeatureC }

    }
}
