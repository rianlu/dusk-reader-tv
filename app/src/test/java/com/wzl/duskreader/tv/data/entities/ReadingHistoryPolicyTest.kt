package com.wzl.duskreader.tv.data.entities

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReadingHistoryPolicyTest {

    @Test
    fun hasReadingHistory_returnsFalseForImportedButUnreadBook() {
        val book = Book(
            title = "Demo",
            path = "/tmp/demo.txt",
            format = "TXT",
            lastReadChapter = 0,
            lastReadPosition = 0,
        )

        assertFalse(book.hasReadingHistory())
    }

    @Test
    fun hasReadingHistory_returnsTrueWhenChapterAdvanced() {
        val book = Book(
            title = "Demo",
            path = "/tmp/demo.txt",
            format = "TXT",
            lastReadChapter = 3,
            lastReadPosition = 0,
        )

        assertTrue(book.hasReadingHistory())
    }

    @Test
    fun hasReadingHistory_returnsTrueWhenWithinChapterOffsetSet() {
        val book = Book(
            title = "Demo",
            path = "/tmp/demo.txt",
            format = "TXT",
            lastReadChapter = 0,
            lastReadPosition = 1200,
        )

        assertTrue(book.hasReadingHistory())
    }

    @Test
    fun progressRatio_isZeroWhenChapterIndexUnknown() {
        val book = Book(
            title = "Demo",
            path = "/tmp/demo.txt",
            format = "TXT",
            lastReadChapter = 5,
            totalSize = 0,
        )

        assertEquals(0f, book.progressRatio(), 0.0001f)
    }

    @Test
    fun progressRatio_dividesByTotalChapterCount() {
        val book = Book(
            title = "Demo",
            path = "/tmp/demo.txt",
            format = "TXT",
            lastReadChapter = 25,
            totalSize = 100, // totalSize 在章节级架构中复用为「总章节数」
        )

        assertEquals(0.25f, book.progressRatio(), 0.0001f)
    }

    @Test
    fun progressRatio_clampsToOneWhenOverrun() {
        val book = Book(
            title = "Demo",
            path = "/tmp/demo.txt",
            format = "TXT",
            lastReadChapter = 200,
            totalSize = 100,
        )

        assertEquals(1f, book.progressRatio(), 0.0001f)
    }
}
