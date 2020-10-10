package com.imgcstmzr.util

import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

/**
 * Declares a requirement on Docker.
 * If no Docker is available this test is skipped.
 */
@Timeout(10, unit = TimeUnit.MINUTES)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
annotation class DockerRequired
