package com.bkahlert.koodies.test.junit

import org.junit.jupiter.api.TestFactory
import org.junit.platform.engine.support.descriptor.ClassSource
import org.junit.platform.engine.support.descriptor.MethodSource
import org.junit.platform.launcher.TestIdentifier
import org.junit.platform.launcher.TestPlan
import java.lang.reflect.GenericDeclaration
import java.lang.reflect.Method

val TestPlan.rootIds: List<String> get() = roots.map { root -> root.uniqueId }
val TestPlan.allTestIdentifiers: List<TestIdentifier> get() = roots.flatMap { getDescendants(it) }

val TestPlan.allTests: List<TestIdentifier> get() = allTestIdentifiers.filter { it.isTest }
val TestPlan.allMethodSources: List<MethodSource> get() = allTests.mapNotNull { it.source.orElse(null) as? MethodSource }
val TestPlan.allTestJavaMethods: List<Method> get() = allMethodSources.map { it.javaMethod }

val TestPlan.allTestContainers: List<TestIdentifier> get() = allTestIdentifiers.filter { it.isTopLevelContainer(this) }
val TestPlan.allContainerSources: List<ClassSource> get() = allTestContainers.mapNotNull { it.source.orElse(null) as? ClassSource }
val TestPlan.allContainerJavaClasses: List<Class<*>> get() = allContainerSources.mapNotNull { it.javaClass }

val TestPlan.allDynamicContainerJavaClasses get() : List<Method> = allTestJavaMethods.withAnnotation<TestFactory>().mapNotNull { it as? Method }
val TestPlan.allEffectiveContainerJavaClasses: List<GenericDeclaration> get() = allContainerJavaClasses + allDynamicContainerJavaClasses

fun TestIdentifier.isTopLevelContainer(testPlan: TestPlan) = testPlan.rootIds.contains(parentId.orElse(null))
