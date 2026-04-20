package com.wzl.duskreader.tv.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.data.model.hasReadingHistory
import com.wzl.duskreader.tv.data.repository.BookRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShelfViewModel(val repository: BookRepository) : ViewModel() {

    companion object {
        private const val TAG = "ShelfViewModel"
    }

    val books: StateFlow<List<Book>> = repository.allBooks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentBooks: StateFlow<List<Book>> = repository.allBooks
        .map { list -> list.filter { it.hasReadingHistory() }.take(5) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val homeScreenState: StateFlow<HomeScreenState> = repository.allBooks
        .map { books -> buildHomeScreenState(books = books) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = buildHomeScreenState(emptyList())
        )


    init {
        com.wzl.duskreader.tv.util.DebugLogger.d(TAG, "ShelfViewModel 已初始化")
    }

    /**
     * 手动触发扫描：立即执行（用于按钮点击）
     */
    fun scanStorage() {
        viewModelScope.launch {
            try {
                repository.scanLocalStorage()
            } catch (e: Exception) {
                Log.e(TAG, "手动扫描失败", e)
            }
        }
    }

    /**
     * 自动触发扫描：优化延迟时间,提升启动体验
     */
    fun scanStorageWithDelay() {
        viewModelScope.launch {
            delay(800) // 减少延迟时间,提升响应速度
            try {
                repository.scanLocalStorage()
            } catch (e: Exception) {
                Log.e(TAG, "后台扫描失败", e)
            }
        }
    }

    fun removeBook(book: Book) {
        viewModelScope.launch { repository.delete(book) }
    }
}

class ShelfViewModelFactory(private val repository: BookRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShelfViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShelfViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
