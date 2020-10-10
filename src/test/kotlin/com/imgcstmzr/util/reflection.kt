package com.imgcstmzr.util

import java.lang.reflect.Method
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.jvm.jvmName

inline fun <reified T : Any> KClass<T>.createInstance(vararg parameters: Any): T =
    requireConstructor(*parameters.map { it::class }.toTypedArray()).call(*parameters) as? T ?: throw IllegalStateException("Implementation error")

fun KClass<*>.createAnyInstance(vararg parameters: Any): Any =
    requireConstructor(*parameters.map { it::class }.toTypedArray()).call(*parameters)

fun KClass<*>.requireConstructor(vararg parameterClasses: KClass<*>): KFunction<Any> =
    constructors.single { it.isInvokableWith(parameterClasses) }

private fun KFunction<Any>.isInvokableWith(parameterClasses: Array<out KClass<*>>): Boolean =
    parameters.size == parameterClasses.size && (parameters.indices).all { i -> parameters[i].isAssignableFrom(parameterClasses[i]) }

private fun KParameter.isAssignableFrom(kClass: KClass<*>?): Boolean = kClass?.let { type.isSubtypeOf(it) } ?: false

private fun KType.isSubtypeOf(kClass: KClass<*>): Boolean = isSubtypeOf(kClass.starProjectedType)


fun KClass<*>.requireCallable(returnType: KType? = null, name: String? = null, parameterTypes: Array<out KClass<*>>): KCallable<*> =
    members.single { it.isInvokableWith(returnType, name, parameterTypes) }

fun KCallable<*>.isInvokableWith(returnType: KType? = null, name: String? = null, parameterTypes: Array<out KClass<*>>?): Boolean =
    returnType?.let { it == this.returnType } ?: true
        && name?.let { it == this.name } ?: true
        && parameterTypes?.let { hasSameParametersTypes(parameterTypes) } ?: true

fun KCallable<*>.hasSameParametersTypes(parameterTypes: Array<out KClass<*>>): Boolean =
    parameters.size == parameterTypes.size && (parameters.indices).all { i -> parameters[i].isAssignableFrom(parameterTypes[i]) }

fun Method.hasSameReturnType(returnType: Class<*>?): Boolean =
    if (returnType != null) this.returnType.isAssignableFrom(returnType)
    else this.returnType == Void::class.java


fun KCallable<*>.format(): String =
    "$receiverTypeSimpleName.$name(${parameters.format()})"

val KCallable<*>.receiverTypeName: String?
    get() = toString().split(" ").getOrNull(1)

val KCallable<*>.receiverTypeSimpleName: String?
    get() = receiverTypeName?.split(".")?.let { it.getOrNull(it.lastIndex - 1) }

fun KFunction<*>.format(): String =
    "$receiverTypeSimpleName.$name(${parameters.format()})"

fun <T : KParameter> Iterable<T>.format(): String = joinToString(", ") { it.format() }

fun <T : KParameter> T?.format(): String = when (this) {
    null -> "null"
    else -> when (type.classifier) {
        is KClass<*> -> (type.classifier as KClass<*>).let {
            it.simpleName ?: it.jvmName.split(".").last()
        }
        else -> type.toString()
    }
}
