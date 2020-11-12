package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.time.sleep
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.time.Duration

private val cachedThreadPool = Executors.newCachedThreadPool()

fun <T> Executor.startAsCompletableFuture(delay: Duration = Duration.ZERO, block: () -> T): CompletableFuture<T> =
    CompletableFuture.supplyAsync({
        delay.sleep()
        block()
    }, this) ?: error("Error creating ${CompletableFuture::class.simpleName}")

fun <T> startAsCompletableFuture(delay: Duration = Duration.ZERO, executor: Executor = cachedThreadPool, block: () -> T): CompletableFuture<T> =
    executor.startAsCompletableFuture(delay = delay, block = block)

fun <T> (() -> T).startAsCompletableFuture(delay: Duration = Duration.ZERO, executor: Executor = cachedThreadPool): CompletableFuture<T> =
    executor.startAsCompletableFuture(delay = delay, block = this)
