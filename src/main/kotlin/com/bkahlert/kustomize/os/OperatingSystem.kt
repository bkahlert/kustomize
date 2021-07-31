package com.bkahlert.kustomize.os

import koodies.unit.Size

/**
 * Representation of an operating system that can be customized.
 */
interface OperatingSystem {

    /**
     * A set of [username] and [password] that can be used to log in to a user session.
     */
    data class Credentials(
        /**
         * The username to be used to log in to a user session.
         */
        val username: String,

        /**
         * The password to be used to log in to a user session.
         */
        val password: String,
    ) {
        companion object {
            private val EMPTY = Credentials("", "")

            /**
             * Factory to create [Credentials] from this string as the username and the specified [password].
             */
            infix fun String.withPassword(password: String): Credentials = Credentials(this, password)

            /**
             * Factory to create empty [Credentials].
             */
            val empty: Credentials = EMPTY
        }
    }

    /**
     * The full name of this [OperatingSystem].
     */
    val fullName: String

    /**
     * Technical name of this [OperatingSystem].
     */
    val name: String

    /**
     * URL that allows an image of this [OperatingSystem] to be downloaded.
     */
    val downloadUrl: String

    /**
     * The approximate [Size] of the actual image file.
     * (And consequently the minimum SD card capacity—plus customizations—needed
     * to store the final image and run it on the actual destined hardware.)
     */
    val approximateImageSize: Size

    /**
     * The [Credentials] to be used to log in to a user session if no others specified.
     */
    val defaultCredentials: Credentials
}
