package com.wzl.duskreader.tv.ui.viewmodel

import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.data.model.hasReadingHistory

data class HomeScreenState(
    val continueReading: Book?,
    val recentBooks: List<Book>,
    val shelfBooks: List<Book>
) {
    val hasAnyBooks: Boolean
        get() = continueReading != null || recentBooks.isNotEmpty() || shelfBooks.isNotEmpty()
}

fun buildHomeScreenState(
    books: List<Book>,
    recentLimit: Int = 6,
    shelfLimit: Int = 12
): HomeScreenState {
    val continueReading = books.firstOrNull { it.hasReadingHistory() }
    val recentBooks = books
        .filter { it.hasReadingHistory() && it.id != continueReading?.id }
        .take(recentLimit)
    val shelfBooks = books
        .filter { it.id != continueReading?.id }
        .take(shelfLimit)

    return HomeScreenState(
        continueReading = continueReading,
        recentBooks = recentBooks,
        shelfBooks = shelfBooks
    )
}
