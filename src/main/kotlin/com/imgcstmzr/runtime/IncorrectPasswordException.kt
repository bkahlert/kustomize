package com.imgcstmzr.runtime

import com.imgcstmzr.util.quoted

class IncorrectPasswordException(credentials: OperatingSystems.Companion.Credentials) :
    IllegalStateException("The entered password ${credentials.password.quoted} is incorrect.")
