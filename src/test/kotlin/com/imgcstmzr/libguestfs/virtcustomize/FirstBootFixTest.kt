package com.imgcstmzr.libguestfs.virtcustomize

import com.imgcstmzr.test.TwoMinutesTimeout
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.withTempDir
import koodies.concurrent.script
import koodies.io.path.executable
import koodies.logging.InMemoryLogger
import koodies.logging.RenderingLogger
import koodies.shell.ShellScript
import koodies.test.copyToDirectory
import koodies.time.sleep
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.time.seconds

@Execution(CONCURRENT)
class FirstBootFixTest {

    private fun Path.runFourScripts(logger: RenderingLogger, customization: Path.() -> Unit = {}): String {
        val outputFile = resolve("test.txt")
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
                        libguestfsFirstBootScript()
                    }
                }
            }
        script(logger) { !"$scriptFile start" }
        return outputFile.readText()
    }

    @TwoMinutesTimeout @Test
    fun `should reproduce incorrect execution order`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
        expectThat(runFourScripts(logger)).isEqualTo("""
            a
            2
            b
            
        """.trimIndent())
    }

    @TwoMinutesTimeout @Test
    fun `should fix execution order`(uniqueId: UniqueId, logger: InMemoryLogger) = withTempDir(uniqueId) {
        expectThat(runFourScripts(logger) { FirstBootFix.copyToDirectory(this).executable = true }).isEqualTo("""
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
private fun Path.script(name: String, shellScript: ShellScript.(Path) -> Unit): Path =
    ShellScript(name) { shellScript(this@script) }.buildTo(resolve(name))

/**
 * Create the given [shellScript] using the given [name] in `this` directory,
 * makes sure it has a proper [ShellScript.shebang] returns its path with
 * 1 second delay to make sure that subsequent calls will have sufficiently
 * different timestamps.
 */
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
 * Create the given the **[libguestfs firstboot script](https://github.com/raspbian-packages/libguestfs/blob/5dd450eaf5dad56b8539a2d324ad1cb5b1277ffa/customize/firstboot.ml)**
 * in `this` directory and returns its path.
 */
private fun Path.libguestfsFirstBootScript() = script("firstboot.sh") { dir ->
    !"""
    #!/bin/sh -
    d=$dir/scripts
    d_done=$dir/scripts-done
    logfile=$dir/../virt-sysprep-firstboot.log
    echo "$0" "$@" 2>&1 | tee -a $`$`logfile
    echo "Scripts dir: $`$`d " 2>&1 | tee -a $`$`logfile
    if test "$1" = "start"
    then
      mkdir -p $`$`d_done
      for f in $`$`d/* ; do
        if test -x $`$`f
        then
          # move the script to the 'scripts-done' directory, so it is not
          # executed again at the next boot
          mv $`$`f $`$`d_done
          echo '=== Running' $`$`f '===' 2>&1 | tee -a $`$`logfile
          $`$`d_done/$(basename $`$`f) 2>&1 | tee -a $`$`logfile
        fi
      done
      rm -f $`$`d_done/*
    fi
    """
}

private val `$` = "$"