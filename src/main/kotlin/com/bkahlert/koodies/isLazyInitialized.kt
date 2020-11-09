package com.bkahlert.koodies

import kotlin.reflect.KProperty0
import kotlin.reflect.jvm.isAccessible

val KProperty0<*>.isLazyInitialized: Boolean
    get() = if (this !is Lazy<*>) true
    else isAccessible.let { originalAccessLevel ->
        isAccessible = true // prevent IllegalAccessException due to JVM private properties access check
        (getDelegate() as Lazy<*>).isInitialized().also { isAccessible = originalAccessLevel }
    }
