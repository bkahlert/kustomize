package com.imgcstmzr.patch.files

import com.imgcstmzr.patch.files.PasswdDocument.Entry
import com.imgcstmzr.util.ImgFixture.Etc.Passwd
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.size

@Execution(CONCURRENT)
class PasswdDocumentTest {
    @Test
    fun `should read passwd`() {
        val passwdFile = Passwd.copyToTemp()
        val passwdDocument = PasswdDocument(passwdFile)
        expectThat(passwdDocument).size.isEqualTo(11)
    }

    @Test
    fun `should find user`() {
        expectThat(PasswdDocument(Passwd.copyToTemp())["daemon"])
            .isEqualTo(Entry("daemon", "*", 18409, 0, "99999", "7", null))
    }

    @Test
    fun `should find user id`() {
        @Suppress("SpellCheckingInspection")
        expectThat(PasswdDocument(Passwd.copyToTemp())["systemd-coredump"]?.userId)
            .isEqualTo(18421)
    }
}
