package com.imgcstmzr.cli

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.parameters.options.OptionWithValues
import com.github.ajalt.clikt.parameters.options.associate
import com.github.ajalt.clikt.parameters.options.option

class TestCli {
    class TestCommand : NoOpCliktCommand() {
        val os = option("os")
        val scripts_: OptionWithValues<String?, String, String> = option("scripts")
        val scripts: Map<String, String> by option().associate()
    }

    companion object {
        val cmd = TestCommand()
        val ctx = Context.build(cmd) {}
    }
}
