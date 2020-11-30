package com.imgcstmzr.runtime

import com.bkahlert.koodies.string.quoted

class IncorrectPasswordException(credentials: OperatingSystem.Credentials) :
    IllegalStateException("The entered password ${credentials.password.quoted} is incorrect.")
