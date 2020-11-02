package com.bkahlert.koodies.nio.file

import java.nio.file.Files
import java.nio.file.Path

var Path.lastModified
    get() = Files.getLastModifiedTime(this)
    set(fileTime) {
        Files.setLastModifiedTime(this, fileTime)
    }
