package com.imgcstmzr.util

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType

inline fun <reified T : Any> KClass<T>.createInstance(vararg parameters: Any): T =
    requireConstructor(*parameters.map { it::class }.toTypedArray()).call(*parameters) as? T ?: throw IllegalStateException("Implementation error")

fun KClass<*>.createAnyInstance(vararg parameters: Any): Any = requireConstructor(*parameters.map { it::class }.toTypedArray()).call(*parameters)

fun KClass<*>.requireConstructor(vararg parameterClasses: KClass<*>): KFunction<Any> = constructors.single { it.isInvokableWith(parameterClasses) }

private fun KFunction<Any>.isInvokableWith(parameterClasses: Array<out KClass<*>>) =
    parameters.size == parameterClasses.size && (parameters.indices).all { i -> parameters[i].isAssignableFrom(parameterClasses[i]) }

private fun KParameter.isAssignableFrom(kClass: KClass<*>): Boolean = type.isSubtypeOf(kClass)

private fun KType.isSubtypeOf(kClass: KClass<*>): Boolean = isSubtypeOf(kClass.starProjectedType)
