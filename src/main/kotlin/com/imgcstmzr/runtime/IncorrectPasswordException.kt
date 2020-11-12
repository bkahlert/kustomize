package com.imgcstmzr.runtime

import com.imgcstmzr.util.quoted

class IncorrectPasswordException(credentials: OperatingSystem.Credentials) :
    IllegalStateException("The entered password ${credentials.password.quoted} is incorrect.")
