package com.bkahlert.koodies.builder

/**
 * Convenience type to easier use [build] accepts.
 */
typealias ListBuilderInit<E> = ListBuilder<E>.() -> Unit


class ListBuilder<in E>(private val list: MutableList<E>) {
    companion object {
        inline fun <reified E> build(init: ListBuilderInit<E>): List<E> =
            mutableListOf<E>().also { ListBuilder(it).apply(init) }
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
}

/**
 * Using `this` [ListBuilderInit] builds a list of elements.
 */
fun <E> ListBuilderInit<E>.build(): List<E> =
    mutableListOf<E>().also { ListBuilder(it).this() }

/**
 * Using `this` [ListBuilderInit] builds a list of elements.
 *
 * As as side effect the result is added to [target].
 */
fun <E> ListBuilderInit<E>.buildTo(target: MutableList<in E>): List<E> =
    build().also { target.addAll(it) }

/**
 * Using `this` [ListBuilderInit] builds a list of elements
 * and applies [transform] to the result.
 */
fun <E, T> ListBuilderInit<E>.build(transform: E.() -> T): List<T> =
    build().map { it.transform() }

/**
 * Using `this` [ListBuilderInit] builds a list of elements
 * and applies [transform] to the result.
 *
 * As as side effect the transformed result is added to [target].
 */
fun <E, T> ListBuilderInit<E>.buildTo(target: MutableCollection<in T>, transform: E.() -> T): List<T> =
    build(transform).also { target.addAll(it) }
