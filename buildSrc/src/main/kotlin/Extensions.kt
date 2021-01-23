@file:Suppress("InvalidPackageDeclaration")

import org.gradle.api.Project

fun Project.propertyOrEmpty(name: String): String {
    val property = findProperty(name) as String?
    return property ?: environmentVariable(name)
}

fun environmentVariable(name: String) = System.getenv(name) ?: ""

val Project.JAVA_HOME
    get() = propertyOrEmpty("org.gradle.java.home")
        ?: System.getenv("JAVA_HOME")
        ?: System.getenv("GRAALVM_HOME")

fun Project.isPublishing() = gradle.startParameter.taskNames.any { it.contains("bintrayUpload") }
