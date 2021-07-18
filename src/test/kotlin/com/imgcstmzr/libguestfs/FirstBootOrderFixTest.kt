package com.imgcstmzr.libguestfs

import koodies.docker.ubuntu
import koodies.exec.RendererProviders
import koodies.io.path.executable
import koodies.junit.UniqueId
import koodies.shell.ShellScript
import koodies.shell.ShellScript.ScriptContext
import koodies.test.TwoMinutesTimeout
import koodies.test.withTempDir
import koodies.time.seconds
import koodies.time.sleep
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText

class FirstBootOrderFixTest {

    private fun Path.runFourScripts(customization: Path.() -> Unit = {}): String {
        val outputFile = "test.txt"
        val scriptFile =
            dir("usr") {
                dir("lib") {
                    dir("virt-sysprep") {
                        dir("scripts") {
                            sh("0001-script-a") < "echo 'a' > '$outputFile'"
                            sh("0002-script-b") < "echo 'b' >> '$outputFile'"
                            sh("0001-script-1") < "echo '1' >> '$outputFile'"
                            sh("0002-script-2") < "echo '2' >> '$outputFile'"
                            customization()
                        }
                        libguestfsFirstBootScript(this@runFourScripts)
                    }
                }
            }
        ubuntu(renderer = RendererProviders.block()) { "$scriptFile start" }
        return resolve(outputFile).readText()
    }

    @TwoMinutesTimeout @Test
    fun `should reproduce incorrect execution order`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(runFourScripts()).isEqualTo("""
            a
            2
            b
            
        """.trimIndent())
    }

    @TwoMinutesTimeout @Test
    fun `should fix execution order`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(runFourScripts { FirstBootOrderFix.copyToDirectory(this).executable = true }).isEqualTo("""
            a
            b
            1
            2
            
        """.trimIndent())
    }
}


/**
 * Creates a directory with the given name in `this` path,
 * runs the given [block] in the newly created directory and returns
 * its result.
 */
private fun <R> Path.dir(name: String, block: Path.() -> R): R =
    with(resolve(name).createDirectories(), block)

/**
 * Create the given [shellScript] using the given [name] in `this` directory
 * and returns its path.
 */
private fun Path.script(name: String, shellScript: ScriptContext.(Path) -> CharSequence): Path =
    ShellScript(name) { shellScript(this@script) }.toFile(resolve(name))

private fun Path.sh(name: String) = ScriptStub(this, name)
private data class ScriptStub(val dir: Path, val name: String) {
    operator fun compareTo(content: String): Int {
        dir.script(name) {
            shebang
            !content
        }.also { 1.seconds.sleep() }
        return 0
    }
}


/**
 * Create the given the
 * **[libguestfs firstboot script](https://github.com/raspbian-packages/libguestfs/blob/5dd450eaf5dad56b8539a2d324ad1cb5b1277ffa/customize/firstboot.ml)**
 * in `this` directory and returns its relative path.
 */
private fun Path.libguestfsFirstBootScript(root: Path) = script("firstboot.sh") { absDir ->
    val dir = root.relativize(absDir)
    """
    #!/bin/sh -
    d=$dir/scripts
    d_done=$dir/scripts-done
    logfile=$dir/../virt-sysprep-firstboot.log
    echo "$0" "$@" 2>&1 | tee -a ${`$`}logfile
    echo "Scripts dir: ${`$`}d " 2>&1 | tee -a ${`$`}logfile
    if test "$1" = "start"
    then
      mkdir -p ${`$`}d_done
      for f in ${`$`}d/* ; do
        if test -x ${`$`}f
        then
          # move the script to the 'scripts-done' directory, so it is not
          # executed again at the next boot
          mv ${`$`}f ${`$`}d_done
          echo '=== Running' ${`$`}f '===' 2>&1 | tee -a ${`$`}logfile
          ${`$`}d_done/$(basename ${`$`}f) 2>&1 | tee -a ${`$`}logfile
        fi
      done
      rm -f ${`$`}d_done/*
    fi
    """
}.let { root.relativize(it) }

@Suppress("ObjectPropertyName")
private const val `$` = "$"
