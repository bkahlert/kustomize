package com.bkahlert.koodies.docker

import com.bkahlert.koodies.concurrent.process.ShellScript


/**
 * Extends [ShellScript] with an entry point to build docker commands.
 */
fun ShellScript.docker(init: DockerImageBuilder.() -> Any): ShellScriptAttachingBuilder =
    ShellScriptAttachingBuilder(this, DockerImageBuilder.build(init))

/**
 * Builder that adds the built commands directly to the [shellScript].
 */
class ShellScriptAttachingBuilder(private val shellScript: ShellScript, private val image: DockerImage) {
    infix fun run(init: DockerRunCommandBuilder.() -> Unit): Unit =
        DockerRunCommandBuilder.build(image, init).run { shellScript.command(this) }
}
