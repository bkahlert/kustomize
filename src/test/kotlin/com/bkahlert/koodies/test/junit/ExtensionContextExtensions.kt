package com.bkahlert.koodies.test.junit

import com.bkahlert.koodies.string.random
import com.imgcstmzr.util.debug.Debug
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestFactory
import org.junit.jupiter.api.TestTemplate
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
 * Name of the current test.
 */
val ExtensionContext.testName: String
    get() :String {
        val separator = " ➜ "
        val name = element.map { parent.map { it.testName }.orElse("") + separator + displayName }.orElse("")
        return if (name.startsWith(separator)) name.substring(separator.length) else name
    }

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

val ExtensionContext.anyDebugTest: Boolean
    get() = allTests.any { it.isA<Debug>() }

/**
 * The container of this test method.
 */
val ExtensionContext.ancestor get() = testMethod.orElse(null)?.declaringClass

/**
 * Contains the ancestor's tests.
 */
private val ExtensionContext.allTests: List<Method>
    get() {
        val root = ancestors.lastOrNull() ?: testClass.orElse(null)
        return root?.descendentContainers?.flatMap {
            it.declaredMethods.toList()
        } ?: emptyList()
    }

/**
 * Contains the ancestors, that is this test's parent container, the parent's parent container, ... up to the root.
 */
val ExtensionContext.ancestors: List<Class<*>>
    get() {
        var ancestor = ancestor
        val containerClasses: MutableList<Class<*>> = mutableListOf()
        while (ancestor != null) {
            containerClasses.add(ancestor)
            ancestor = ancestor.declaringClass
        }
        return containerClasses
    }

/**
 * Contains the ancestors, that is this test's parent container, the parent's parent container, ... up to the root.
 */
val Class<*>.descendentContainers: List<Class<*>>
    get() = mutableListOf(this) + declaredClasses.flatMap { it.descendentContainers }


/**
 * Whether this [Method] is a [Test].
 */
val Optional<Method>?.isTest get() = this?.orElse(null).isTest

/**
 * Whether this [Method] is a [Test].
 */
val Method?.isTest get() = isA<Test>() || isA<TestFactory>() || isA<TestTemplate>()

