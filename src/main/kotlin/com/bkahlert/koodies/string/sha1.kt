package com.bkahlert.koodies.string

import java.security.MessageDigest

val CharSequence.sha1: String
    get() = MessageDigest.getInstance("SHA-1").digest(toString().toByteArray()).joinToString("") { byte -> "%02x".format(byte) }