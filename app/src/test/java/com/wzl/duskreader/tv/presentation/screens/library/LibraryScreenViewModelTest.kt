package com.wzl.duskreader.tv.presentation.screens.library

import com.wzl.duskreader.tv.data.entities.Book
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryScreenViewModelTest {

    @Test
    fun presentLibraryBooks_sortsByRecentReadWithUnreadAtEnd() {
        val books = listOf(
            testBook(id = 1, title = "B", lastReadPosition = 800_000, lastReadTime = 200),
            testBook(id = 2, title = "A", lastReadPosition = 0, lastReadTime = 500),
            testBook(id = 3, title = "C", lastReadPosition = 400_000, lastReadTime = 300),
        )

        val result = presentLibraryBooks(books, LibrarySortOption.RecentRead, LibraryFormatFilter.All)

        assertEquals(listOf(3L, 1L, 2L), result.map { it.id })
    }

    @Test
    fun presentLibraryBooks_filtersByFormatBeforeSorting() {
        val books = listOf(
            testBook(id = 1, title = "Gamma", format = "TXT", importedAt = 100),
            testBook(id = 2, title = "Alpha", format = "EPUB", importedAt = 300),
            testBook(id = 3, title = "Beta", format = "EPUB", importedAt = 200),
        )

        val result = presentLibraryBooks(books, LibrarySortOption.Title, LibraryFormatFilter.Epub)

        assertEquals(listOf(2L, 3L), result.map { it.id })
    }

    @Test
    fun presentLibraryBooks_sortsByRecentImportDescending() {
        val books = listOf(
            testBook(id = 1, title = "Old", importedAt = 100),
            testBook(id = 2, title = "New", importedAt = 500),
            testBook(id = 3, title = "Middle", importedAt = 300),
        )

        val result = presentLibraryBooks(books, LibrarySortOption.RecentImport, LibraryFormatFilter.All)

        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    private fun testBook(
        id: Long,
        title: String,
        format: String = "TXT",
        importedAt: Long = 0,
        lastReadPosition: Int = 0,
        lastReadTime: Long = 0,
    ): Book {
        return Book(
            id = id,
            title = title,
            path = "/tmp/$title.txt",
            format = format,
            importedAt = importedAt,
            lastReadPosition = lastReadPosition,
            lastReadTime = lastReadTime,
        )
    }
}
