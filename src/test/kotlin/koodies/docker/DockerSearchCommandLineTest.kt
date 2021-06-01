package koodies.docker

import koodies.docker.DockerSearchCommandLine.DockerSeachResult
import strikt.api.Assertion
import strikt.api.DescribeableBuilder

val Assertion.Builder<DockerSeachResult>.image: DescribeableBuilder<DockerImage>
    get() = get("image") { image }
val Assertion.Builder<DockerSeachResult>.description: DescribeableBuilder<String>
    get() = get("description") { description }
val Assertion.Builder<DockerSeachResult>.stars: DescribeableBuilder<Int>
    get() = get("starCount") { stars }

fun Assertion.Builder<DockerSeachResult>.isOfficial() =
    assert("is official") {
        when (it.official) {
            true -> pass()
            else -> fail("not official")
        }
    }

fun Assertion.Builder<DockerSeachResult>.isAutomated() =
    assert("is automated") {
        when (it.automated) {
            true -> pass()
            else -> fail("not official")
        }
    }
