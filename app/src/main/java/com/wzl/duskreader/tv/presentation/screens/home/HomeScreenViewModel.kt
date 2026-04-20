package com.wzl.duskreader.tv.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.entities.BookList
import com.wzl.duskreader.tv.data.repositories.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    bookRepository: BookRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeScreenUiState> = combine(
        bookRepository.getRecentBooks(limit = 10),
        bookRepository.getAllBooks(),
    ) { recent, all ->
        HomeScreenUiState.Ready(recentBooks = recent, allBooks = all)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeScreenUiState.Loading,
    )
}

sealed interface HomeScreenUiState {
    data object Loading : HomeScreenUiState
    data object Error : HomeScreenUiState
    data class Ready(
        val recentBooks: BookList,
        val allBooks: BookList,
    ) : HomeScreenUiState
}
