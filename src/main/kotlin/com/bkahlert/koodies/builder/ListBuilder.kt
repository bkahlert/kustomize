package com.bkahlert.koodies.builder

class ListBuilder<E>(private val list: MutableList<E>) {
    companion object {
        inline fun <reified E> build(init: ListBuilder<E>.() -> Unit): List<E> =
            mutableListOf<E>().also { ListBuilder(it).apply(init) }

        inline fun <reified T, reified E> build(init: ListBuilder<T>.() -> Unit, transform: (T) -> E): List<E> =
            mutableListOf<T>().also { ListBuilder(it).apply(init) }.map(transform)
    }

    operator fun E.unaryPlus() {
        list.add(this)
    }

    operator fun Iterable<E>.unaryPlus() {
        list.addAll(this)
    }

    operator fun Array<E>.unaryPlus() {
        list.addAll(this)
    }

    operator fun Sequence<E>.unaryPlus() {
        list.addAll(this)
    }
}
