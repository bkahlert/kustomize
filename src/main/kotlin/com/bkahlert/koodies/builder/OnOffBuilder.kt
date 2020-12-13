package com.bkahlert.koodies.builder

/**
 * `On`/`Off` style builder for [Boolean].
 *
 * @sample OnOffBuilderSamples.directUse
 * @sample OnOffBuilderSamples.indirectUse
 */
object OnOffBuilder {
    fun build(init: OnOffBuilderInit): Boolean =
        OnOffBuilder.init()

    val on get() = true
    val off get() = false
}

fun OnOffBuilderInit.build(): Boolean =
    OnOffBuilder.this()

fun OnOffBuilderInit.build(collection: MutableCollection<Boolean>): Boolean =
    build().also { collection.add(it) }

fun <T> OnOffBuilderInit.build(transformOnTrue: () -> T): T? =
    build().let { if (it) transformOnTrue() else null }

fun <T> OnOffBuilderInit.buildTo(collection: MutableCollection<T>, transformOnTrue: () -> T): T? =
    build(transformOnTrue)?.also { collection.add(it) }

fun <K, V> OnOffBuilderInit.buildTo(map: MutableMap<K, V>, transformOnTrue: () -> Pair<K, V>): Pair<K, V>? =
    build().let { if (it) transformOnTrue() else null }?.also { map[it.first] = it.second }

/**
 * Type [TrueFalseBuilder.build] accepts.
 */
typealias OnOffBuilderInit = OnOffBuilder.() -> Boolean

@Suppress("UNUSED_VARIABLE")
private object OnOffBuilderSamples {

    fun directUse() {

        val toggle = OnOffBuilder.build { off }

    }

    fun indirectUse() {

        fun builderAcceptingFunction(init: OnOffBuilderInit) {
            val toggle = OnOffBuilder.build(init)
            println("Et voilà, $toggle")
        }

        val toggle = builderAcceptingFunction { on }
    }
}
