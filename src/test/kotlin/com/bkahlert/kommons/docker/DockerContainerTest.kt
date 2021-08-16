package com.bkahlert.kommons.docker

import com.bkahlert.kommons.docker.DockerContainer.State
import com.bkahlert.kommons.docker.DockerContainer.State.Existent.Exited
import strikt.api.Assertion.Builder
import strikt.assertions.isA


inline fun <reified T : State> Builder<DockerContainer>.hasState(): Builder<DockerContainer> =
    compose("status") {
        get { containerState }.isA<T>()
    }.then { if (allPassed) pass() else fail() }

inline fun <reified T : State> Builder<DockerContainer>.hasState(
    crossinline statusAssertion: Builder<T>.() -> Unit,
): Builder<DockerContainer> =
    compose("status") {
        get { containerState }.isA<T>().statusAssertion()
    }.then { if (allPassed) pass() else fail() }

val Builder<DockerContainer>.name get(): Builder<String> = get("name") { name }

val Builder<Exited>.exitCode get(): Builder<Int?> = get("exit code") { exitCode }
