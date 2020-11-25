package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.time.sleep
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.time.Duration

private val cachedThreadPool = Executors.newCachedThreadPool()

fun <T> Executor.startAsCompletableFuture(
    delay: Duration = Duration.ZERO,
    name: String? = null,
    block: () -> T,
): CompletableFuture<T> =
    CompletableFuture.supplyAsync({
        delay.sleep()
        name?.let { withThreadName(name, block) } ?: block()
    }, this) ?: error("Error creating ${CompletableFuture::class.simpleName}")

fun <T> startAsCompletableFuture(
    delay: Duration = Duration.ZERO,
    name: String? = null,
    executor: Executor = cachedThreadPool,
    block: () -> T,
): CompletableFuture<T> =
    executor.startAsCompletableFuture(delay = delay, name = name, block = block)

fun <T> (() -> T).startAsCompletableFuture(
    delay: Duration = Duration.ZERO,
    name: String? = null,
    executor: Executor = cachedThreadPool,
): CompletableFuture<T> =
    executor.startAsCompletableFuture(delay = delay, name = name, block = this)

fun <T> withThreadName(temporaryName: String, block: () -> T): T =
    with(Thread.currentThread()) {
        val oldName = name.also { name = temporaryName }
        runCatching(block).also { name = oldName }.getOrThrow()
    }
