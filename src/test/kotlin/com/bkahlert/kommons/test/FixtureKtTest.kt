package com.bkahlert.kommons.test

import com.bkahlert.kommons.io.InMemoryFile
import com.bkahlert.kommons.io.InMemoryTextFile
import strikt.api.Assertion

val <T : InMemoryFile> Assertion.Builder<T>.name get() = get("name %s") { name }
val <T : InMemoryTextFile> Assertion.Builder<T>.text get() = get("text %s") { text }
val <T : InMemoryFile> Assertion.Builder<T>.data get() = get("data %s") { data }
