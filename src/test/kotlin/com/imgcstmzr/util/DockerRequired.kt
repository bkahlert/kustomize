package com.imgcstmzr.util

import com.bkahlert.koodies.test.junit.E2E
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import java.util.concurrent.TimeUnit

/**
 * Declares a requirement on Docker.
 * If no Docker is available this test is skipped.
 */
@E2E
@Timeout(15, unit = TimeUnit.MINUTES)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@ExtendWith(FixtureResolverExtension::class)
annotation class DockerRequired
