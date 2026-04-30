package com.wzl.duskreader.tv.presentation.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.BookList
import com.wzl.duskreader.tv.data.entities.hasReadingHistory
import com.wzl.duskreader.tv.data.repositories.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

enum class LibrarySortOption(val label: String) {
    RecentRead("最近阅读"),
    RecentImport("最近导入"),
    Title("文件名"),
}

enum class LibraryFormatFilter(val label: String) {
    All("全部"),
    Txt("TXT"),
    Epub("EPUB"),
}

@HiltViewModel
class LibraryScreenViewModel @Inject constructor(
    bookRepository: BookRepository,
) : ViewModel() {

    private val sortOption = MutableStateFlow(LibrarySortOption.RecentRead)
    private val formatFilter = MutableStateFlow(LibraryFormatFilter.All)

    val uiState: StateFlow<LibraryScreenUiState> = combine(
        bookRepository.getAllBooks(),
        sortOption,
        formatFilter,
    ) { books, sort, filter ->
        LibraryScreenUiState.Ready(
            books = presentLibraryBooks(books, sort, filter),
            totalBookCount = books.size,
            selectedSort = sort,
            selectedFilter = filter,
        ) as LibraryScreenUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LibraryScreenUiState.Loading,
    )

    fun updateSort(sort: LibrarySortOption) {
        sortOption.value = sort
    }

    fun updateFilter(filter: LibraryFormatFilter) {
        formatFilter.value = filter
    }
}

internal fun presentLibraryBooks(
    books: BookList,
    sort: LibrarySortOption,
    filter: LibraryFormatFilter,
): BookList {
    val filtered = books.filterByFormat(filter)
    return when (sort) {
        LibrarySortOption.RecentRead -> filtered.sortedWith(
            compareByDescending<Book> { it.hasReadingHistory() }
                .thenByDescending { it.lastReadTime }
                .thenBy { it.title.lowercase() },
        )

        LibrarySortOption.RecentImport -> filtered.sortedWith(
            compareByDescending<Book> { it.importedAt }
                .thenBy { it.title.lowercase() },
        )

        LibrarySortOption.Title -> filtered.sortedBy { it.title.lowercase() }
    }
}

private fun BookList.filterByFormat(filter: LibraryFormatFilter): BookList {
    return when (filter) {
        LibraryFormatFilter.All -> this
        LibraryFormatFilter.Txt -> filter { it.format.equals("TXT", ignoreCase = true) }
        LibraryFormatFilter.Epub -> filter { it.format.equals("EPUB", ignoreCase = true) }
    }
}

sealed interface LibraryScreenUiState {
    data object Loading : LibraryScreenUiState
    data class Ready(
        val books: BookList,
        val totalBookCount: Int,
        val selectedSort: LibrarySortOption,
        val selectedFilter: LibraryFormatFilter,
    ) : LibraryScreenUiState {
        val hasAnyBooks: Boolean = totalBookCount > 0
    }
}
