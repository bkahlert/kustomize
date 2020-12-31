package com.imgcstmzr.runtime

import koodies.text.quoted

class IncorrectPasswordException(credentials: OperatingSystem.Credentials) :
    IllegalStateException("The entered password ${credentials.password.quoted} is incorrect.")
