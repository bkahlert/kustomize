package com.bkahlert.koodies.nio.file

import java.nio.file.Path
import java.nio.file.attribute.FileTime
import java.time.Instant

var Path.lastModifiedInstant: Instant
    get() = lastModified.toInstant()
    set(value) {
        lastModified = FileTime.from(value)
    }
