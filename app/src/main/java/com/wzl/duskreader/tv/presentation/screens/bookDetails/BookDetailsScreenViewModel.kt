package com.wzl.duskreader.tv.presentation.screens.bookDetails

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.repositories.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class BookDetailsScreenViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BookRepository,
) : ViewModel() {

    val uiState = savedStateHandle
        .getStateFlow<String?>(BookDetailsScreen.BookIdBundleKey, null)
        .map { raw ->
            val id = raw?.toLongOrNull()
            if (id == null) {
                BookDetailsScreenUiState.Error
            } else {
                val book = repository.getBookById(id)
                if (book == null) {
                    BookDetailsScreenUiState.Error
                } else {
                    BookDetailsScreenUiState.Done(book)
                }
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BookDetailsScreenUiState.Loading,
        )
}

sealed interface BookDetailsScreenUiState {
    data object Loading : BookDetailsScreenUiState
    data object Error : BookDetailsScreenUiState
    data class Done(val book: Book) : BookDetailsScreenUiState
}
