package com.wzl.duskreader.tv.data.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UploadFilePolicyTest {

    @Test
    fun resolveSafeFilename_stripsPathTraversalAndNormalizesExtension() {
        val safeName = UploadFilePolicy.resolveSafeFilename("../../Novel.TXT")

        assertEquals("Novel.txt", safeName)
    }

    @Test
    fun resolveSafeFilename_rejectsUnsupportedExtension() {
        val safeName = UploadFilePolicy.resolveSafeFilename("malware.apk")

        assertNull(safeName)
    }

    @Test
    fun resolveSafeFilename_rejectsMissingReadableBasename() {
        val safeName = UploadFilePolicy.resolveSafeFilename(".txt")

        assertNull(safeName)
    }

    @Test
    fun resolveSafeFilename_replacesIllegalCharacters() {
        val safeName = UploadFilePolicy.resolveSafeFilename("book?name.epub")

        assertEquals("book_name.epub", safeName)
    }
}
