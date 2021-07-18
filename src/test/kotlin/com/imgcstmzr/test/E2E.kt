package com.imgcstmzr.test

import com.imgcstmzr.libguestfs.LibguestfsImage
import com.imgcstmzr.os.OperatingSystemProcess.Companion.DockerPiImage
import com.imgcstmzr.test.E2E.Companion.NAME
import koodies.docker.DockerRequiring
import koodies.test.ThirtyMinutesTimeout
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Timeout
import org.junit.jupiter.api.parallel.ResourceAccessMode.READ_WRITE
import org.junit.jupiter.api.parallel.ResourceLock

/**
 * JUnit 5 annotation to explicitly denote [end-to-end tests](https://wiki.c2.com/?EndToEndPrinciple).
 *
 * Meta-annotated with @[DockerRequiring] and @[ResourceLock] and a @[Timeout] of 15 minutes.
 */
@ThirtyMinutesTimeout
@DockerRequiring([LibguestfsImage::class, DockerPiImage::class])
@ResourceLock(DockerResources.SERIAL, mode = READ_WRITE)
@Tag(NAME)
@Target(
    AnnotationTarget.ANNOTATION_CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER,
    AnnotationTarget.CLASS
)
@Retention(AnnotationRetention.RUNTIME)
annotation class E2E {
    companion object {
        const val NAME = "E2E"
    }
}
