package com.imgcstmzr.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.sources.ExperimentalValueSourceApi

@ExperimentalValueSourceApi
class TestCli {
    class TestCommand : NoOpCliktCommand() {
        init {
            context { valueSource = Config4kValueSource.from(javaClass.getResource("/sample.conf").readText()) }
        }

        val os = option("os")
        val scripts_: OptionWithValues<String?, String, String> = option("scripts")
        val scripts: Map<String, String> by option().associate()
    }

    companion object {
        val cmd = TestCommand()
        val ctx = Context.build(cmd) {}
    }
}
