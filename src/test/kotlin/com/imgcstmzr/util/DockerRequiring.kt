package com.imgcstmzr.util

import com.bkahlert.koodies.docker.DockerResources
import com.bkahlert.koodies.test.junit.Slow
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.parallel.ResourceAccessMode.READ
import org.junit.jupiter.api.parallel.ResourceLock

/**
 * Declares a requirement on Docker.
 * If no Docker is available this test is skipped.
 *
 * The [Timeout] is automatically increased to 2 minutes.
 */
@Slow
@ResourceLock(DockerResources.SERIAL, mode = READ)
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@ExtendWith(FixtureResolverExtension::class)
annotation class DockerRequiring
