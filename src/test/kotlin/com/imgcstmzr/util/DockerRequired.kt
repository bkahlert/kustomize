package com.imgcstmzr.util

import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import java.util.concurrent.TimeUnit

/**
 * Declares a requirement on Docker.
 * If no Docker is available this test is skipped.
 */
@Execution(ExecutionMode.SAME_THREAD)
@Timeout(10, unit = TimeUnit.MINUTES)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@ExtendWith(FixtureResolverExtension::class)
annotation class DockerRequired
