package com.imgcstmzr.patch.files

import com.imgcstmzr.patch.files.PasswdDocument.Entry
import com.imgcstmzr.test.ImgClassPathFixture.Etc.Passwd
import com.imgcstmzr.test.UniqueId
import com.imgcstmzr.withTempDir
import koodies.test.copyToDirectory
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode.CONCURRENT
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import strikt.assertions.size

@Execution(CONCURRENT)
class PasswdDocumentTest {

    @Test
    fun `should read passwd`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        val passwdFile = Passwd.copyToDirectory(this)
        val passwdDocument = PasswdDocument(passwdFile)
        expectThat(passwdDocument).size.isEqualTo(11)
    }

    @Test
    fun `should find user`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        expectThat(PasswdDocument(Passwd.copyToDirectory(this))["daemon"])
            .isEqualTo(Entry("daemon", "*", 18409, 0, "99999", "7", null))
    }

    @Test
    fun `should find user id`(uniqueId: UniqueId) = withTempDir(uniqueId) {
        @Suppress("SpellCheckingInspection")
        expectThat(PasswdDocument(Passwd.copyToDirectory(this))["systemd-coredump"]?.userId)
            .isEqualTo(18421)
    }
}
