package com.bkahlert.koodies.nio.file

import com.bkahlert.koodies.string.quoted
import java.nio.file.Path

val Path.quoted get() = serialized.quoted
