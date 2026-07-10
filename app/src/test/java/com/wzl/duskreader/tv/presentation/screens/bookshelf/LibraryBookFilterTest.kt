package com.wzl.duskreader.tv.presentation.screens.bookshelf

import com.wzl.duskreader.tv.data.entities.Book
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryBookFilterTest {

    @Test
    fun search_matchesTitleAndAuthorBeforeApplyingLimit() {
        val books = (1..LIBRARY_LIMIT).map { index ->
            book(id = index.toLong(), title = "普通书籍 $index", importedAt = index.toLong())
        } + book(
            id = 999,
            title = "暮色故事",
            author = "Alice",
            importedAt = 0,
        )

        assertEquals(
            listOf("暮色故事"),
            filterAndSortLibraryBooks(
                books = books,
                query = "暮色",
                formatFilter = LibraryFormatFilter.All,
                sort = LibrarySort.Imported,
            ).map { it.title },
        )
        assertEquals(
            listOf("暮色故事"),
            filterAndSortLibraryBooks(
                books = books,
                query = "ali",
                formatFilter = LibraryFormatFilter.All,
                sort = LibrarySort.Imported,
            ).map { it.title },
        )
    }

    @Test
    fun formatFilter_keepsOnlyRequestedBookKind() {
        val books = listOf(
            book(id = 1, title = "TXT", format = "TXT"),
            book(id = 2, title = "EPUB", format = "EPUB"),
        )

        assertEquals(
            listOf("TXT"),
            filterAndSortLibraryBooks(books, "", LibraryFormatFilter.Txt, LibrarySort.Imported)
                .map { it.title },
        )
        assertEquals(
            listOf("EPUB"),
            filterAndSortLibraryBooks(books, "", LibraryFormatFilter.Epub, LibrarySort.Imported)
                .map { it.title },
        )
    }

    @Test
    fun sorting_supportsImportedReadAndTitleOrder() {
        val books = listOf(
            book(id = 1, title = "Beta", importedAt = 100, lastReadTime = 300),
            book(
                id = 2,
                title = "alpha",
                importedAt = 200,
                lastReadTime = 100,
                lastReadPosition = 1,
            ),
        )

        assertEquals(
            listOf(2L, 1L),
            filterAndSortLibraryBooks(books, "", LibraryFormatFilter.All, LibrarySort.Imported)
                .map { it.id },
        )
        assertEquals(
            listOf(2L, 1L),
            filterAndSortLibraryBooks(books, "", LibraryFormatFilter.All, LibrarySort.Read)
                .map { it.id },
        )
        assertEquals(
            listOf(2L, 1L),
            filterAndSortLibraryBooks(books, "", LibraryFormatFilter.All, LibrarySort.Title)
                .map { it.id },
        )
    }

    private fun book(
        id: Long,
        title: String,
        author: String? = null,
        format: String = "TXT",
        importedAt: Long = 0,
        lastReadTime: Long = 0,
        lastReadPosition: Int = 0,
    ) = Book(
        id = id,
        title = title,
        author = author,
        path = "/tmp/$id.$format",
        format = format,
        importedAt = importedAt,
        lastReadTime = lastReadTime,
        lastReadPosition = lastReadPosition,
    )
}
