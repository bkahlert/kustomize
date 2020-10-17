package com.bkahlert.koodies.string

import java.net.URL

fun String.asSystemResourceUrl(): URL = ClassLoader.getSystemResources(this).nextElement()
