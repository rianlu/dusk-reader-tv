package com.wzl.duskreader.tv.presentation.screens.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.BookKind
import com.wzl.duskreader.tv.data.entities.BookList
import com.wzl.duskreader.tv.data.entities.hasReadingHistory
import com.wzl.duskreader.tv.data.entities.kind
import com.wzl.duskreader.tv.data.repositories.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

internal const val LIBRARY_LIMIT = 240

enum class LibraryFormatFilter(val label: String) {
    All("全部格式"),
    Txt("仅 TXT"),
    Epub("仅 EPUB");

    fun next(): LibraryFormatFilter = entries[(ordinal + 1) % entries.size]
}

enum class LibrarySort(val label: String) {
    Imported("最近导入"),
    Read("最近阅读"),
    Title("书名排序");

    fun next(): LibrarySort = entries[(ordinal + 1) % entries.size]
}

@HiltViewModel
class BookshelfScreenViewModel @Inject constructor(
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _rescanState = MutableStateFlow<RescanState>(RescanState.Idle)
    val rescanState: StateFlow<RescanState> = _rescanState.asStateFlow()

    private val searchQuery = MutableStateFlow("")
    private val formatFilter = MutableStateFlow(LibraryFormatFilter.All)
    private val librarySort = MutableStateFlow(LibrarySort.Imported)

    val uiState: StateFlow<BookshelfUiState> = combine(
        bookRepository.getRecentBooks(limit = 8),
        bookRepository.getAllBooks(),
        searchQuery,
        formatFilter,
        librarySort,
    ) { recent, all, query, filter, sort ->
        BookshelfUiState.Ready(
            recentBooks = recent,
            allBooks = all,
            libraryBooks = filterAndSortLibraryBooks(all, query, filter, sort),
            searchQuery = query,
            formatFilter = filter,
            librarySort = sort,
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        // Loading 态只渲染背景（无内容），避免空书架卡片闪一下。
        // Room 查询通常 < 10ms，Loading 态几乎不可见。切 tab 时 ViewModel 不重建，不经过此状态。
        initialValue = BookshelfUiState.Loading,
    )

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun cycleFormatFilter() {
        formatFilter.value = formatFilter.value.next()
    }

    fun cycleLibrarySort() {
        librarySort.value = librarySort.value.next()
    }

    fun rescanLibrary() {
        viewModelScope.launch {
            if (_rescanState.value is RescanState.Scanning) return@launch
            _rescanState.value = RescanState.Scanning
            runCatching { bookRepository.scanLocalStorage() }
                .onSuccess { imported -> _rescanState.value = RescanState.Done(imported) }
                .onFailure { error ->
                    _rescanState.value = RescanState.Failure(error.message ?: "未知错误")
                }
        }
    }
}

sealed interface RescanState {
    data object Idle : RescanState
    data object Scanning : RescanState
    data class Done(val imported: Int) : RescanState
    data class Failure(val message: String) : RescanState
}

sealed interface BookshelfUiState {
    data object Loading : BookshelfUiState
    data class Ready(
        val recentBooks: BookList,
        val allBooks: BookList,
        val libraryBooks: BookList,
        val searchQuery: String,
        val formatFilter: LibraryFormatFilter,
        val librarySort: LibrarySort,
    ) : BookshelfUiState
}

internal fun filterAndSortLibraryBooks(
    books: BookList,
    query: String,
    formatFilter: LibraryFormatFilter,
    sort: LibrarySort,
): BookList {
    val normalizedQuery = query.trim()
    val filtered = books.asSequence()
        .filter { book ->
            when (formatFilter) {
                LibraryFormatFilter.All -> true
                LibraryFormatFilter.Txt -> book.kind() == BookKind.Novel
                LibraryFormatFilter.Epub -> book.kind() == BookKind.Epub
            }
        }
        .filter { book ->
            normalizedQuery.isEmpty() ||
                book.title.contains(normalizedQuery, ignoreCase = true) ||
                book.author.orEmpty().contains(normalizedQuery, ignoreCase = true)
        }

    val comparator = when (sort) {
        LibrarySort.Imported -> compareByDescending<Book> { it.importedAt }
        LibrarySort.Read -> compareByDescending<Book> { it.hasReadingHistory() }
            .thenByDescending { it.lastReadTime }
            .thenByDescending { it.importedAt }
        LibrarySort.Title -> compareBy(String.CASE_INSENSITIVE_ORDER) { book: Book -> book.title }
            .thenByDescending { it.importedAt }
    }
    return filtered.sortedWith(comparator).take(LIBRARY_LIMIT).toList()
}
