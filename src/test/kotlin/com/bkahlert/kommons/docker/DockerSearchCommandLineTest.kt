package com.bkahlert.kommons.docker

import com.bkahlert.kommons.docker.DockerSearchCommandLine.DockerSearchResult
import strikt.api.Assertion
import strikt.api.DescribeableBuilder


val Assertion.Builder<DockerSearchResult>.image: DescribeableBuilder<DockerImage>
    get() = get("image") { image }
val Assertion.Builder<DockerSearchResult>.description: DescribeableBuilder<String>
    get() = get("description") { description }
val Assertion.Builder<DockerSearchResult>.stars: DescribeableBuilder<Int>
    get() = get("starCount") { stars }

fun Assertion.Builder<DockerSearchResult>.isOfficial() =
    assert("is official") {
        when (it.official) {
            true -> pass()
            else -> fail("not official")
        }
    }

fun Assertion.Builder<DockerSearchResult>.isAutomated() =
    assert("is automated") {
        when (it.automated) {
            true -> pass()
            else -> fail("not official")
        }
    }
