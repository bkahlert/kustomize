package com.bkahlert.koodies.process

import kotlin.streams.asSequence

object Processes {
    val current: ProcessHandle get() = ProcessHandle.current()

    val descendants: Sequence<ProcessHandle>
        get() = current.descendants().filter { it.isAlive && it.info().startInstant().isPresent }.asSequence()

    val children: Sequence<ProcessHandle>
        get() = descendants.filter { it.parent().map { it.pid() == current.pid() }.orElse(false) }

    val recentChildren: List<ProcessHandle>
        get() = children.toList().sortedByDescending { it.info().startInstant().get() }

    val mostRecentChild: ProcessHandle
        get() = recentChildren.firstOrNull()
            ?: throw IllegalStateException("This process $current has no recent child processes!\nChildren: ${children.toList()}")
}
