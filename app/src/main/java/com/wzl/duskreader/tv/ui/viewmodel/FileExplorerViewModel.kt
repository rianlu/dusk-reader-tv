package com.wzl.duskreader.tv.ui.viewmodel

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.data.repository.BookRepository
import com.wzl.duskreader.tv.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

/**
 * 文件管理视图模型：负责浏览本地存储和导入文件。
 */
class FileExplorerViewModel(private val repository: BookRepository) : ViewModel() {

    private val _currentPath = MutableStateFlow<File?>(null)
    val currentPath = _currentPath.asStateFlow()

    private val _files = MutableStateFlow<List<File>>(emptyList())
    val files = _files.asStateFlow()

    private val _isImporting = MutableStateFlow(false)
    val isImporting = _isImporting.asStateFlow()

    init {
        // 初始进入公共文档目录
        val initialDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        loadDirectory(initialDir)
    }

    /**
     * 加载指定目录的文件列表
     */
    fun loadDirectory(directory: File) {
        if (!directory.exists() || !directory.isDirectory) return

        _currentPath.value = directory
        
        viewModelScope.launch(Dispatchers.IO) {
            val list = directory.listFiles()?.filter { 
                !it.isHidden && (it.isDirectory || it.extension.lowercase() in setOf("txt", "epub"))
            }?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() })) ?: emptyList()
            
            _files.value = list
        }
    }

    /**
     * 返回上一级
     */
    fun navigateUp() {
        val current = _currentPath.value ?: return
        val parent = current.parentFile
        if (parent != null && parent.canRead()) {
            loadDirectory(parent)
        }
    }

    /**
     * 导入书籍文件
     */
    fun importFile(file: File, onComplete: (Book?) -> Unit) {
        if (file.isDirectory) return
        
        viewModelScope.launch(Dispatchers.IO) {
            _isImporting.value = true
            try {
                // 检查是否已存在
                val existing = repository.findBookByPath(file.absolutePath)
                if (existing != null) {
                    onComplete(existing)
                } else {
                    val book = Book(
                        title = file.nameWithoutExtension,
                        path = file.absolutePath,
                        format = file.extension.uppercase(),
                        totalSize = file.length()
                    )
                    repository.insert(book)
                    onComplete(book)
                }
            } catch (e: Exception) {
                DebugLogger.e("FileExplorer", "导入文件失败: ${file.name}", e)
                onComplete(null)
            } finally {
                _isImporting.value = false
            }
        }
    }
}

class FileExplorerViewModelFactory(private val repository: BookRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FileExplorerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FileExplorerViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
