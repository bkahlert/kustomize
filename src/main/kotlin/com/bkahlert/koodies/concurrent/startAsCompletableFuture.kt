package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.time.sleep
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

private val cachedThreadPool = Executors.newCachedThreadPool()

@OptIn(ExperimentalTime::class)
fun <T> Executor.startAsCompletableFuture(delay: Duration = Duration.ZERO, block: () -> T): CompletableFuture<T> =
    CompletableFuture.supplyAsync({
        delay.sleep()
        block()
    }, this) ?: error("Error creating ${CompletableFuture::class.simpleName}")

@OptIn(ExperimentalTime::class)
fun <T> startAsCompletableFuture(delay: Duration = Duration.ZERO, executor: Executor = cachedThreadPool, block: () -> T): CompletableFuture<T> =
    executor.startAsCompletableFuture(delay = delay, block = block)

@OptIn(ExperimentalTime::class)
fun <T> (() -> T).startAsCompletableFuture(delay: Duration = Duration.ZERO, executor: Executor = cachedThreadPool): CompletableFuture<T> =
    executor.startAsCompletableFuture(delay = delay, block = this)
