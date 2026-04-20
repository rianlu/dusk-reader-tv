package com.wzl.duskreader.tv.data.entities

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingHistoryPolicyTest {
    @Test
    fun sanityCheck_runsUnitTestInJvm() {
        assertTrue(true)
    }

    @Test
    fun hasReadingHistory_returnsFalseForImportedButUnreadBook() {
        val book = Book(
            title = "Demo",
            path = "/tmp/demo.txt",
            format = "TXT",
            lastReadPosition = 0
        )

        assertFalse(book.hasReadingHistory())
    }

    @Test
    fun hasReadingHistory_returnsTrueWhenSavedProgressExists() {
        val book = Book(
            title = "Demo",
            path = "/tmp/demo.txt",
            format = "TXT",
            lastReadPosition = 1200
        )

        assertTrue(book.hasReadingHistory())
    }
}
