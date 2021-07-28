package com.bkahlert.koodies

import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

val KProperty0<*>.isLazyInitialized: Boolean
    get() {
        // Prevent IllegalAccessException from JVM access check on private properties.
        val originalAccessLevel = isAccessible
        isAccessible = true
        val isLazyInitialized = (getDelegate() as? Lazy<*>)?.isInitialized() ?: return true
        // Reset access level.
        isAccessible = originalAccessLevel
        return isLazyInitialized
    }
