package com.imgcstmzr.patch.files

import java.nio.file.Path
import kotlin.io.path.readLines

class PasswdDocument(entries: List<Entry>) :
    List<PasswdDocument.Entry> by entries {

    constructor(file: Path) : this(file.readLines()
        .map { it.split(":") }
        .map {
            Entry(
                username = it[0],
                password = it[1],
                userId = it[2].toIntOrNull(),
                groupId = it[3].toIntOrNull(),
                userIdInfo = it[4].toNotBlankOrNull(),
                homeDirectory = it[5].toNotBlankOrNull(),
                shell = it[6].toNotBlankOrNull()
            )
        })

    data class Entry(
        val username: String,
        val password: String,
        val userId: Int?,
        val groupId: Int?,
        val userIdInfo: String?,
        val homeDirectory: String?,
        val shell: String?,
    )

    operator fun get(username: String): Entry? =
        find { it.username == username }
}

fun String.toNotBlankOrNull() = takeUnless { isBlank() }
