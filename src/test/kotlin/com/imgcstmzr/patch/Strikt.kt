package com.imgcstmzr.patch

import com.imgcstmzr.process.Guestfish
import com.imgcstmzr.process.Output
import com.imgcstmzr.runtime.HasStatus
import com.imgcstmzr.runtime.OperatingSystem
import com.imgcstmzr.runtime.RunningOS
import com.imgcstmzr.runtime.log.BlockRenderingLogger
import com.imgcstmzr.util.asRootFor
import com.imgcstmzr.util.quoted
import strikt.api.Assertion
import java.nio.file.Path

fun Assertion.Builder<Guestfish>.path(guestPath: String): Assertion.Builder<Path> = path(Path.of(guestPath))

fun Assertion.Builder<Guestfish>.path(guestPath: Path): Assertion.Builder<Path> = get("running $guestPath") {
    run(Guestfish.copyOutCommands(listOf(guestPath)))

    val root = guestRootOnHost
    root.asRootFor(guestPath)
}

inline fun Assertion.Builder<Path>.mounted(
    logger: BlockRenderingLogger<Unit>,
    crossinline assertion: Assertion.Builder<Guestfish>.() -> Unit,
): Assertion.Builder<Path> {
    get("mounted") {
        get { Guestfish(this, logger) }.apply(assertion)
    }
    return this
}

fun Assertion.Builder<String>.isEqualTo(expected: String) =
    assert("is equal to ${expected.quoted}") {
        val actual = it
        when (actual == expected) {
            true -> pass()
            else -> fail()
        }
    }

inline fun <reified T : OperatingSystem> Assertion.Builder<Path>.booted(
    logger: BlockRenderingLogger<Unit>,
    crossinline assertion: RunningOS.(Output) -> (Output) -> Boolean,
): Assertion.Builder<Path> {
    val os = T::class.objectInstance ?: error("Invalid OS")
    get("booted $os") {
        var processor: ((Output) -> Boolean)? = null
        var shuttingDown = false

        os.bootToUserSession("Booting to assert $this", this, logger) { output ->
            logger.logLine(output, listOf(object : HasStatus {
                override fun status(): String = if (shuttingDown) "shutting down" else "testing"
            }))
            if (!shuttingDown) {
                if (processor == null) processor = this@bootToUserSession.assertion(output)
                if (processor != null) {
                    // returning true means test was successful -> you can shutdown
                    if ((processor?.invoke(output)) != false) {
                        shuttingDown = true
                    }
                }
                if (shuttingDown) shutdown()
            }
        }
    }

    return this
}
