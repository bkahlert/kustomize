package com.bkahlert.koodies.builder

interface TypesafeBuilder<B : Any, T : Any> {
    fun b(): B
    fun t(): T
}

//inline fun <reified B : Any, reified T : Any, reified E : Any> TypesafeBuilder<B, T>.build(noinline init: B.() -> Unit): T {
//    return build<E>(init)
//}

interface TypesafeListBuilder : TypesafeBuilder<ListBuilder<*>, List<*>> {
    fun <E> build(b: ListBuilder<E>, init: ListBuilder<E>.() -> Unit): List<E>
}


object X : TypesafeListBuilder {
    override fun <E> build(b: ListBuilder<E>, init: ListBuilder<E>.() -> Unit): List<E> {
        return emptyList<E>()
    }

    override fun b(): ListBuilder<*> {
        TODO("Not yet implemented")
    }

    override fun t(): List<*> {
        TODO("Not yet implemented")
    }

//        override fun <B : ListBuilder<List<E>>, E : Any> build(init: B.() -> Unit): List<E> =
//            mutableListOf<E>().also { ListBuilder(it).apply(init) }

//        inline fun <reified E> build(init: ListBuilderInit<E>): List<E> =
//            mutableListOf<E>().also { ListBuilder(it).apply(init) }
}

//fun redirects(init: ListBuilderInit<Any>) {
//    with(X) {
//        build(init)
//    }
//}

