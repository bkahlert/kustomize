package com.imgcstmzr.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.sources.ValueSource

class EmptyValueSource : ValueSource {
    override fun getValues(context: Context, option: Option): List<ValueSource.Invocation> = emptyList()
}
