package com.imgcstmzr

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.Option
import com.github.ajalt.clikt.sources.ExperimentalValueSourceApi
import com.github.ajalt.clikt.sources.ValueSource

@OptIn(ExperimentalValueSourceApi::class)
class DelegatingValueSource<T : ValueSource>(var delegate: T? = null) : ValueSource {
    override fun getValues(context: Context, option: Option): List<ValueSource.Invocation> {
        return delegate?.getValues(context, option) ?: emptyList()
    }
}
