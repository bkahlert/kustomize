package koodies.docker

import koodies.docker.DockerContainer.State
import koodies.docker.DockerContainer.State.Existent.Exited
import strikt.api.Assertion.Builder
import strikt.assertions.isA

inline fun <reified T : State> Builder<DockerContainer>.hasState(): Builder<DockerContainer> =
    compose("status") {
        get { state }.isA<T>()
    }.then { if (allPassed) pass() else fail() }

inline fun <reified T : State> Builder<DockerContainer>.hasState(
    crossinline statusAssertion: Builder<T>.() -> Unit,
): Builder<DockerContainer> =
    compose("status") {
        get { state }.isA<T>().statusAssertion()
    }.then { if (allPassed) pass() else fail() }

val Builder<DockerContainer>.name get(): Builder<String> = get("name") { name }

val Builder<Exited>.exitCode get(): Builder<Int?> = get("exit code") { exitCode }
