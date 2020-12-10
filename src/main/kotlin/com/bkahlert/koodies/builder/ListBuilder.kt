package com.bkahlert.koodies.builder


/**
 * Builds a list [E] by
 * 1) instantiating an instance of its receiver [B] *(using [B]'s **required zero-arg constructor**)*
 * 2) apply `this` initializer to it
 * 3) retrieving the result using `(B) -> T`.
 *
 * @return the build instance
 */
inline fun <reified B : (B) -> List<E>, reified E : Any> (B.() -> Unit).buildList(): List<E> {
    val zeroArgConstructors = B::class.java.declaredConstructors.filter { it.parameterCount == 0 }
    val builder: B = zeroArgConstructors.singleOrNull()?.newInstance() as? B
        ?: throw IllegalArgumentException("${B::class.simpleName} has no zero-arg constructor and cannot be used to create a list of ${E::class.simpleName}.")
    return builder.apply(this).let { it.invoke(it) }
}

/**
 * Builds a list of [E] and adds it to [target] by
 * 1) instantiating an instance of its receiver [B] *(using [B]'s **required zero-arg constructor**)*
 * 2) apply `this` initializer to it
 * 3) retrieving the result using `(B) -> T`.
 *
 * @return the build instance
 */
inline fun <reified B : (B) -> List<E>, reified E : Any> (B.() -> Unit).buildListTo(target: MutableCollection<in E>): List<E> =
    buildList().also { target.addAll(it) }

/**
 * Builds a list of [E] and [transform]s it to [U] by
 * 1) instantiating an instance of its receiver [B] *(using [B]'s **required zero-arg constructor**)*
 * 2) apply `this` initializer to it
 * 3) retrieving the result using `(B) -> T`
 * 4) applying [transform].
 *
 * @return the transformed instance
 */
inline fun <reified B : (B) -> List<E>, reified E : Any, reified U> (B.() -> Unit).buildList(transform: E.() -> U): List<U> =
    buildList().map(transform)

/**
 * Builds a list of [E] and adds the to [U] [transform]ed instance to [target] by
 * 1) instantiating an instance of its receiver [B] *(using [B]'s **required zero-arg constructor**)*
 * 2) apply `this` initializer to it
 * 3) retrieving the result using `(B) -> T`
 * 4) applying [transform].
 *
 * @return the transformed instance
 */
inline fun <reified B : (B) -> List<E>, reified E : Any, reified U> (B.() -> Unit).buildListTo(target: MutableCollection<in U>, transform: E.() -> U): List<U> =
    buildList(transform).also { target.addAll(it) }



/**
 * Convenience type to easier use [buildMap] accepts.
 */
typealias ListBuilderInit<E> = ListBuilder<E>.() -> Unit

open class ListBuilder<E> : (ListBuilder<E>) -> List<E> by { it.list } {

    protected val list: MutableList<E> = mutableListOf()

    companion object {
        inline fun <reified E> build(noinline init: ListBuilder<E>.() -> Unit): List<E> = init.build()
    }

    operator fun E.unaryPlus() {
        list.add(this)
    }

    operator fun Unit.plus(element: E) {
        list.add(element)
    }

    operator fun List<E>.unaryPlus() {
        list.addAll(this)
    }

    operator fun Array<out E>.unaryPlus() {
        list.addAll(this)
    }

    operator fun Sequence<E>.unaryPlus() {
        list.addAll(this)
    }

    override fun toString(): String = "ListBuilder[list=$list]"
}
