package com.wzl.duskreader.tv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.data.reader.TxtReaderEngine
import com.wzl.duskreader.tv.data.repository.BookRepository
import com.wzl.duskreader.tv.util.DebugLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 阅读器视图模型 (ReaderViewModel)：管理阅读核心逻辑
 * 负责加载章节、翻页控制以及进度同步。
 */
class ReaderViewModel(
    private val book: Book,
    private val repository: BookRepository
) : ViewModel() {

    // 当前章节内容
    private val _content = MutableStateFlow("正在加载...")
    val content = _content.asStateFlow()

    // 章节目录
    private val _chapters = MutableStateFlow<List<TxtReaderEngine.Chapter>>(emptyList())
    val chapters = _chapters.asStateFlow()

    // 当前章节索引
    private val _currentChapterIndex = MutableStateFlow(book.lastReadChapter)
    val currentChapterIndex = _currentChapterIndex.asStateFlow()

    private var readerEngine: TxtReaderEngine? = null

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val file = File(book.path)
                if (file.exists()) {
                    val engine = TxtReaderEngine(file)
                    readerEngine = engine
                    val chapterList = engine.parseChapters()
                    _chapters.value = chapterList
                    
                    if (chapterList.isEmpty()) {
                        _content.value = "未能解析出章节目录，请检查文件内容。"
                    } else {
                        // 加载保存的章节
                        loadChapter(book.lastReadChapter)
                    }
                } else {
                    _content.value = "文件不存在或路径已变更: ${book.path}"
                }
            } catch (e: Exception) {
                DebugLogger.e("ReaderVM", "解析书籍失败: ${book.title}", e)
                _content.value = "书籍加载失败: ${e.message ?: "未知错误"}"
            }
        }
    }

    /**
     * 加载指定索引的章节内容
     */
    fun loadChapter(index: Int) {
        val engine = readerEngine ?: return
        val chapterList = _chapters.value
        if (index in chapterList.indices) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val chapter = chapterList[index]
                    val text = engine.readChapterContent(chapter)
                    
                    // 处理文本格式：增加段落间距并清理多余空行
                    val formattedText = text.lines()
                        .filter { it.isNotBlank() }
                        .joinToString("\n\n")
                    
                    _content.value = if (formattedText.isBlank()) "该章节暂无内容" else formattedText
                    _currentChapterIndex.value = index
                    
                    // 同步进度到数据库
                    repository.update(book.copy(
                        lastReadChapter = index,
                        lastReadTime = System.currentTimeMillis()
                    ))
                } catch (e: Exception) {
                    DebugLogger.e("ReaderVM", "加载章节失败 index: $index", e)
                    _content.value = "章节内容加载失败: ${e.message}"
                }
            }
        }
    }

    /**
     * 下一章
     */
    fun nextChapter() {
        if (_currentChapterIndex.value < _chapters.value.size - 1) {
            loadChapter(_currentChapterIndex.value + 1)
        }
    }

    /**
     * 上一章
     */
    fun prevChapter() {
        if (_currentChapterIndex.value > 0) {
            loadChapter(_currentChapterIndex.value - 1)
        }
    }
}
