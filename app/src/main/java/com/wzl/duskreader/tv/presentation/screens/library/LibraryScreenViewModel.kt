package com.wzl.duskreader.tv.presentation.screens.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.entities.BookList
import com.wzl.duskreader.tv.data.repositories.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class LibraryScreenViewModel @Inject constructor(
    bookRepository: BookRepository,
) : ViewModel() {

    val uiState: StateFlow<LibraryScreenUiState> = bookRepository.getAllBooks()
        .map { books -> LibraryScreenUiState.Ready(books) as LibraryScreenUiState }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = LibraryScreenUiState.Loading,
        )
}

sealed interface LibraryScreenUiState {
    data object Loading : LibraryScreenUiState
    data class Ready(val books: BookList) : LibraryScreenUiState
}
