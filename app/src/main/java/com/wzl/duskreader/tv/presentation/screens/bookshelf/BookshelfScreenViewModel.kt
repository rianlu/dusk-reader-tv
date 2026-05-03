package com.wzl.duskreader.tv.presentation.screens.bookshelf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.entities.BookList
import com.wzl.duskreader.tv.data.repositories.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class BookshelfScreenViewModel @Inject constructor(
    private val bookRepository: BookRepository,
) : ViewModel() {

    private val _rescanState = MutableStateFlow<RescanState>(RescanState.Idle)
    val rescanState: StateFlow<RescanState> = _rescanState.asStateFlow()

    val uiState: StateFlow<BookshelfUiState> = combine(
        bookRepository.getRecentBooks(limit = 3),
        bookRepository.getAllBooks(),
    ) { recent, all ->
        BookshelfUiState.Ready(
            recentBooks = recent,
            allBooks = all,
        ) as BookshelfUiState
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = BookshelfUiState.Loading,
    )

    fun rescanLibrary() {
        if (_rescanState.value is RescanState.Scanning) return
        viewModelScope.launch {
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
    ) : BookshelfUiState
}
