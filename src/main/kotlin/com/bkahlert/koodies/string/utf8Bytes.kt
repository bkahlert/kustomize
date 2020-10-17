package com.bkahlert.koodies.string

import com.bkahlert.koodies.unit.Size
import com.bkahlert.koodies.unit.bytes

val CharSequence.utf8Bytes: Size get() = utf8.size.bytes
