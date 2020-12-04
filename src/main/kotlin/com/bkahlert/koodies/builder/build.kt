package com.bkahlert.koodies.builder

/**
 * Builds `this` 'Builder' by invoking it.
 *
 * @return the result
 */
inline fun <reified T> Producer<T>.build(): T =
    this()

/**
 * Builds `this` 'Builder' by invoking it.
 *
 * As as side effect the result is added to [target].
 *
 * @return the result
 */
inline fun <reified T> Producer<T>.buildTo(target: MutableCollection<in T>): T =
    build().also { target.add(it) }

/**
 * Builds `this` 'Builder' by invoking it and applying [transform] on the result.
 *
 * @return the transformed result
 */
inline fun <reified T, reified U> Producer<T>.build(transform: T.() -> U): U =
    build().run(transform)

/**
 * Builds `this` 'Builder' by invoking it and applying [transform] on the result.
 *
 * As as side effect the transformed result is added to [target].
 *
 * @return the transformed result
 */
inline fun <reified T, reified U> Producer<T>.buildTo(target: MutableCollection<in U>, transform: T.() -> U): U =
    build(transform).also { target.add(it) }


/**
 * Type for lambdas that produce instances of `T` with no need for arguments.
 */
typealias Producer<T> = () -> T
