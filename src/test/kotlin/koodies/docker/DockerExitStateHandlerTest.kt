package koodies.docker

import koodies.exec.Process.ExitState
import strikt.api.Assertion.Builder

fun Builder<ExitState>.isSuccessful(): Builder<ExitState> =
    assert("exit state represents success") { actual ->
        when (actual.successful) {
            true -> pass(actual.successful)
            null -> fail("process did not terminate,yet")
            false -> fail("process failed: $actual")
        }
    }


fun Builder<ExitState>.isFailed(): Builder<ExitState> =
    assert("exit state represents failed") { actual ->
        when (actual.successful) {
            true -> fail("process did not fail: $actual")
            null -> fail("process did not terminate,yet")
            false -> pass(actual.successful)
        }
    }
