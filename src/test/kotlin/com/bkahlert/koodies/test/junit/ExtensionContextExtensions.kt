package com.bkahlert.koodies.test.junit

import com.bkahlert.koodies.string.random
import com.imgcstmzr.util.debug.Debug
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtensionContext
import java.lang.reflect.AnnotatedElement
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.util.Optional

/**
 * Checks if the test class, method, test template, etc. of the current scope fulfills the requirements implemented by the provided [tester].
 */
fun ExtensionContext.element(tester: AnnotatedElement.() -> Boolean) = element.map(tester).orElse(false)

/**
 * Contains an ID valid for the whole test plan run.
 */
val ExtensionContext.uniqueName
    get() = element.map {
        when (it) {
            is Class<*> -> it.simpleName
            is Method -> it.name
            is Executable -> it.name
            else -> it.toString()
        }
    }.orElseGet {
        "unknown $this"
    } + " - " + String.random(8)

val ExtensionContext.hasDebugChildren: Boolean
    get() = testClass.children.any { it.isA<Debug>() }

val ExtensionContext.hasDebugSiblings: Boolean
    get() = testMethod.siblings.any { it.isA<Debug>() }

val ExtensionContext.isDebug: Boolean
    get() = testMethod.isDebug

val Optional<Method>?.isDebug get() = this?.orElse(null).isDebug
val Optional<Method>?.isTest get() = this?.orElse(null).isTest
val Method?.isDebug get() = this.isA<Debug>()
val Method?.isTest get() = this.isA<Test>()

val Optional<Class<*>>.children
    get() = orElse(null)?.declaredMethods?.filter { it.isTest }?.toList() ?: emptyList()

val Optional<Method>.siblings
    get() = orElse(null)?.declaringClass?.declaredMethods?.filter { it.isTest }?.toList() ?: emptyList()
