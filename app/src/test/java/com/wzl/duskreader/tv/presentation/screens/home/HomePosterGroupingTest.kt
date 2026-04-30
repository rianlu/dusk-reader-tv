package com.wzl.duskreader.tv.presentation.screens.home

import com.wzl.duskreader.tv.data.entities.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePosterGroupingTest {

    @Test
    fun buildHomeShelves_keepsRecentFirstAndLimitsShelfSize() {
        val books = (1L..12L).map { id ->
            Book(
                id = id,
                title = "Book $id",
                path = "/tmp/$id.txt",
                format = "TXT",
                importedAt = id,
                lastReadPosition = if (id <= 3) 100 else 0,
                lastReadTime = 1000L - id,
            )
        }

        val shelves = buildHomeShelves(books, books.take(3))

        assertEquals("继续阅读", shelves.first().title)
        assertEquals(3, shelves.first().books.size)
        assertTrue(shelves.drop(1).all { it.books.size <= 10 })
    }
}
