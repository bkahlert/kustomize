package koodies.ext.debug

val stackTrace get() = Thread.currentThread().stackTrace

fun <T> stackTrace(transform: StackTraceElement.() -> T): Sequence<T> =
    Thread.currentThread().stackTrace.asSequence().map(transform)

val StackTraceElement.clazz get() = Class.forName(className)

val StackTraceElement.method get() = clazz.declaredMethods.single { it.name == methodName }
