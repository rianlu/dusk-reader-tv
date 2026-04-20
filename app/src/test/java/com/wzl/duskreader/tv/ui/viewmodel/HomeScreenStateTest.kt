package com.wzl.duskreader.tv.ui.viewmodel

import com.wzl.duskreader.tv.data.model.Book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomeScreenStateTest {
    @Test
    fun buildHomeScreenState_prioritizesContinueReadingAndRemovesDuplicateFromRecentRail() {
        val continueBook = book(id = 1, title = "继续阅读", lastReadPosition = 250)
        val recentBook = book(id = 2, title = "最近阅读", lastReadPosition = 120)
        val unreadBook = book(id = 3, title = "新书", lastReadPosition = 0)

        val state = buildHomeScreenState(
            books = listOf(continueBook, recentBook, unreadBook),
            recentLimit = 6,
            shelfLimit = 12
        )

        assertEquals(continueBook, state.continueReading)
        assertEquals(listOf(recentBook), state.recentBooks)
        assertEquals(listOf(recentBook, unreadBook), state.shelfBooks)
    }

    @Test
    fun buildHomeScreenState_fallsBackToShelfWhenNoReadingHistoryExists() {
        val unreadBook = book(id = 11, title = "未读书籍", lastReadPosition = 0)

        val state = buildHomeScreenState(books = listOf(unreadBook))

        assertNull(state.continueReading)
        assertEquals(emptyList<Book>(), state.recentBooks)
        assertEquals(listOf(unreadBook), state.shelfBooks)
    }

    private fun book(
        id: Long,
        title: String,
        lastReadPosition: Int
    ) = Book(
        id = id,
        title = title,
        path = "/tmp/$title.txt",
        format = "TXT",
        lastReadPosition = lastReadPosition
    )
}
