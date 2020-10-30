package com.bkahlert.koodies.concurrent

import com.bkahlert.koodies.time.sleep
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

private val cachedThreadPool = Executors.newCachedThreadPool()

@OptIn(ExperimentalTime::class)
fun <T> startAsCompletableFuture(delay: Duration = Duration.ZERO, executor: Executor = cachedThreadPool, block: () -> T): CompletableFuture<T> =
    CompletableFuture.supplyAsync({
        delay.sleep()
        block()
    }, executor) ?: error("Error creating ${CompletableFuture::class.simpleName}")

@OptIn(ExperimentalTime::class)
fun <T> (() -> T).startAsCompletableFuture(delay: Duration = Duration.ZERO, executor: Executor = cachedThreadPool): CompletableFuture<T> =
    startAsCompletableFuture(delay = delay, executor = executor, block = this)
