package com.bkahlert.koodies.string


import java.security.MessageDigest

val CharSequence.md5: String
    get() = MessageDigest.getInstance("MD5").digest(toString().toByteArray()).joinToString("") { byte -> "%02x".format(byte) }
