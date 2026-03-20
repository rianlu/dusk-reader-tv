package com.wzl.duskreader.tv.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.data.repository.BookRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 书架视图模型 (ViewModel)：负责维护 UI 状态
 * 处理来自 UI 的书籍加载请求，并响应数据库中数据的变动。
 */
class ShelfViewModel(val repository: BookRepository) : ViewModel() {

    companion object {
        private const val TAG = "ShelfViewModel"
    }

    /**
     * 将 Flow 转换为 UI 可观察的 StateFlow。
     * 当数据库中有新书导入或进度更新时，书架界面会自动刷新。
     */
    val books: StateFlow<List<Book>> = repository.allBooks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /**
     * 最近阅读书籍流：仅展示最近读取过的 5 本（已打开过且 lastReadTime > 0）。
     */
    val recentBooks: StateFlow<List<Book>> = repository.allBooks
        .map { list -> 
            list.filter { it.lastReadTime > 0 }.take(5)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // 应用启动时自动扫描本地存储中的书籍文件
        scanStorage()
    }

    /**
     * 扫描本地存储，将新发现的书籍文件导入数据库。
     * 扫描完成后，书架会通过 Flow 自动刷新。
     */
    fun scanStorage() {
        viewModelScope.launch {
            try {
                val count = repository.scanLocalStorage()
                if (count > 0) {
                    Log.d(TAG, "自动导入了 $count 本书")
                }
            } catch (e: Exception) {
                Log.e(TAG, "扫描本地存储失败", e)
            }
        }
    }

    /**
     * 临时导入书籍测试功能 (后续会改为文件选择器)
     */
    fun addDummyBook(title: String) {
        viewModelScope.launch {
            val book = Book(
                title = title,
                path = "/sdcard/Download/$title.txt",
                format = "TXT"
            )
            repository.insert(book)
        }
    }
    
    /**
     * 移除书籍记录
     */
    fun removeBook(book: Book) {
        viewModelScope.launch {
            repository.delete(book)
        }
    }
}

/**
 * ViewModel 工厂类：由于 ShelfViewModel 需要传入外部 Repository 参数，
 * 必须使用 Factory 进行实例化。
 */
class ShelfViewModelFactory(private val repository: BookRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ShelfViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ShelfViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
