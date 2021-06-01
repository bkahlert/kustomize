package koodies

import strikt.api.Assertion.Builder
import strikt.api.DescribeableBuilder
import java.nio.file.Path

val Builder<Path>.content: DescribeableBuilder<String>
    get() = get("file content") { this.toFile().readText() } // TODO use path

fun Builder<Path>.content(assertion: Builder<String>.() -> Unit): Builder<String> =
    content.and { assertion() }
