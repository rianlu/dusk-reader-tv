package com.wzl.duskreader.tv.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.BookList
import com.wzl.duskreader.tv.data.entities.hasReadingHistory
import com.wzl.duskreader.tv.data.repositories.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class PosterShelf(
    val title: String,
    val books: BookList,
)

data class HomeStage(
    val featuredBook: Book?,
    val shelves: List<PosterShelf>,
)

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    bookRepository: BookRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeScreenUiState> = combine(
        bookRepository.getRecentBooks(limit = 10),
        bookRepository.getAllBooks(),
    ) { recent, all ->
        val stageBook = recent.firstOrNull() ?: all.maxByOrNull { it.importedAt }
        HomeScreenUiState.Ready(
            recentBooks = recent,
            allBooks = all,
            stage = HomeStage(
                featuredBook = stageBook,
                shelves = buildHomeShelves(all, recent),
            ),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeScreenUiState.Loading,
    )
}

internal fun buildHomeShelves(
    allBooks: BookList,
    recentBooks: BookList,
): List<PosterShelf> {
    val recentImports = allBooks
        .sortedByDescending { it.importedAt }
        .take(10)
    val unreadBooks = allBooks
        .filterNot { it.hasReadingHistory() }
        .take(10)

    return buildList {
        if (recentBooks.isNotEmpty()) add(PosterShelf("继续阅读", recentBooks.take(10)))
        if (recentImports.isNotEmpty()) add(PosterShelf("最近导入", recentImports))
        if (unreadBooks.isNotEmpty()) add(PosterShelf("未开始阅读", unreadBooks))
        if (allBooks.isNotEmpty()) add(PosterShelf("全部书库", allBooks.take(10)))
    }
}

sealed interface HomeScreenUiState {
    data object Loading : HomeScreenUiState
    data object Error : HomeScreenUiState
    data class Ready(
        val recentBooks: BookList,
        val allBooks: BookList,
        val stage: HomeStage,
    ) : HomeScreenUiState
}
