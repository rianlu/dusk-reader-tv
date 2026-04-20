package com.wzl.duskreader.tv.ui.viewmodel

import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
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
import java.io.File

/**
 * 8.0 增强型：支持全书字节级精准跳转与进度计算
 */
class ReaderViewModel(
    private val book: Book,
    private val repository: BookRepository
) : ViewModel() {

    private val _pages = MutableStateFlow<List<String>>(emptyList())
    val pages = _pages.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    // 真正的全书进度 (0.0 - 1.0)
    private val _totalProgress = MutableStateFlow(0f)
    val totalProgress = _totalProgress.asStateFlow()

    // 目录数据
    data class Chapter(val title: String, val offset: Int)
    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters = _chapters.asStateFlow()
    private val _currentChapterTitle = MutableStateFlow("开始")
    val currentChapterTitle = _currentChapterTitle.asStateFlow()

    private val _isPaging = MutableStateFlow(false)
    val isPaging = _isPaging.asStateFlow()
    private val _snapToEndAfterPaging = MutableStateFlow(false)
    val snapToEndAfterPaging = _snapToEndAfterPaging.asStateFlow()

    private var fullText: String = ""
    // 当前窗口在全书中的起始字符位置
    private var windowStartOffset: Int = 0
    // 当前页在全书中的起始位置 (用于真正保存进度)
    private var currentPageOffset: Int = 0
    // 缓存每一页的字数，用于精确计算进度
    private var pageLengths = mutableListOf<Int>()

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch(Dispatchers.IO) {
            val file = File(book.path)
            if (file.exists()) {
                val engine = TxtReaderEngine(file)
                val rawText = engine.readFullContent()
                
                // 排版预处理：清理多余空行，并强制每个段落首行缩进两个全角空格
                fullText = rawText
                    .split("\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n\n") { "　　$it" }
                
                // 1. 扫描目录
                scanChapters()

                // 2. 读取进度并定位初始窗口
                // 兼容性逻辑：如果是旧的 0-100 进度，则按百分比计算；如果是新的百万分位进度，则更精确
                val savedPos = book.lastReadPosition
                currentPageOffset = decodeSavedPosition(
                    savedPos = savedPos,
                    fullTextLength = fullText.length
                )
                
                windowStartOffset = currentPageOffset
                
                _isLoaded.value = true
                updateProgress()
                updateCurrentChapterTitle()
                DebugLogger.d("ReaderVM", "文本加载完成，长度: ${fullText.length}, 初始偏移: $currentPageOffset")
            } else {
                _pages.value = listOf("文件不存在: ${book.path}")
            }
        }
    }

    private fun scanChapters() {
        val regex = Regex("^\\s*(第.{1,9}[章节回部集卷]).*", RegexOption.MULTILINE)
        val found = mutableListOf<Chapter>()
        regex.findAll(fullText).forEach { match ->
            found.add(Chapter(match.groupValues[0].trim().take(20), match.range.first))
        }
        
        // 如果没搜到，给个默认的“开始”
        if (found.isEmpty()) {
            found.add(Chapter("开始", 0))
        }
        _chapters.value = found
        _currentChapterTitle.value = found.first().title
    }

    private fun updateProgress() {
        if (fullText.isNotEmpty()) {
            _totalProgress.value = currentPageOffset.toFloat() / fullText.length
        }
        updateCurrentChapterTitle()
    }

    private fun updateCurrentChapterTitle() {
        val chapter = _chapters.value.lastOrNull { it.offset <= currentPageOffset } ?: _chapters.value.firstOrNull()
        if (chapter != null) {
            _currentChapterTitle.value = chapter.title
        }
    }

    /**
     * 跳转到指定字符偏移量
     */
    fun jumpToOffset(offset: Int) {
        val target = offset.coerceIn(0, fullText.length)
        currentPageOffset = target
        windowStartOffset = target
        updateProgress()
        // 此处需要加载新内容，先标记为正在分页，并清空当前页以便重新触发加载
        _pages.value = emptyList()
    }

    /**
     * 全书分页逻辑：优化防抖延迟,提升翻页响应速度
     */
    fun performPaging(
        textMeasurer: TextMeasurer,
        containerConstraints: Constraints,
        textStyle: TextStyle
    ) {
        if (!_isLoaded.value || fullText.isEmpty()) return
        if (_isPaging.value) return

        viewModelScope.launch(Dispatchers.Default) {
            _isPaging.value = true
            val startTime = System.currentTimeMillis()
            try {
                // 减少防抖延迟,提升响应速度
                kotlinx.coroutines.delay(100)

                val pagesList = mutableListOf<String>()
                val lengths = mutableListOf<Int>()

                // 窗口大小约 40,000 字
                val windowLimit = 40000
                var cursor = windowStartOffset
                val totalLength = fullText.length
                val endOfWindow = (cursor + windowLimit).coerceAtMost(totalLength)

                var remainingText = fullText.substring(cursor, endOfWindow)

                // 每次分页最多生成 100 页
                while (remainingText.isNotEmpty() && pagesList.size < 100) {
                    val layoutResult: TextLayoutResult = textMeasurer.measure(
                        text = remainingText,
                        style = textStyle,
                        constraints = containerConstraints,
                        softWrap = true
                    )

                    val breakIndex = if (layoutResult.hasVisualOverflow) {
                        layoutResult.getLineEnd(layoutResult.lineCount - 1, true)
                    } else {
                        remainingText.length
                    }

                    if (breakIndex <= 0) {
                        val fallback = remainingText.take(1)
                        pagesList.add(fallback)
                        lengths.add(fallback.length)
                        remainingText = remainingText.drop(1)
                    } else {
                        val pageContent = remainingText.substring(0, breakIndex)
                        pagesList.add(pageContent)
                        lengths.add(pageContent.length)
                        remainingText = remainingText.substring(breakIndex)
                    }
                }

                pageLengths = lengths
                _pages.value = pagesList
                DebugLogger.d(
                    "ReaderVM",
                    "窗口分页完成 [${windowStartOffset}..${windowStartOffset + lengths.sum()}], 耗时: ${System.currentTimeMillis() - startTime}ms"
                )
            } finally {
                _isPaging.value = false
            }
        }
    }

    /**
     * 翻页时同步更新当前精确偏移
     */
    fun onPageChanged(pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= pageLengths.size) return
        
        // 计算当前页在全书中的绝对位置
        var offsetInWindow = 0
        for (i in 0 until pageIndex) {
            offsetInWindow += pageLengths[i]
        }
        currentPageOffset = windowStartOffset + offsetInWindow
        updateProgress()
    }

    /**
     * 外部 UI 发现到了最后，可以调用此方法加载下一段
     */
    fun loadNextWindow() {
        if (_isPaging.value || pageLengths.isEmpty()) return
        windowStartOffset += pageLengths.sum()
        currentPageOffset = windowStartOffset
        _snapToEndAfterPaging.value = false
        // 不要主动清空 _pages，由 performPaging 结束后替换
        // 但我们需要一种方式通知 UI 重新调用 performPaging
        // 方案：我们将 _pages 设为一个极简单的特殊值，或者通过一个专门的 trigger
        _pages.value = pages.value.take(1) // 暂时保留一页，防止闪烁
        _pages.value = emptyList() // 仍然需要 empty 触发 performPaging 加载，但我们会优化 UI 层次
    }

    /**
     * 加载上一段
     */
    fun loadPreviousWindow() {
        if (_isPaging.value) return
        windowStartOffset = (windowStartOffset - 35000).coerceAtLeast(0)
        currentPageOffset = windowStartOffset
        _snapToEndAfterPaging.value = true
        _pages.value = emptyList()
    }

    fun consumeSnapToEndFlag() {
        _snapToEndAfterPaging.value = false
    }

    fun bookTitle(): String = book.title

    fun saveProgress() {
        viewModelScope.launch(Dispatchers.IO) {
            val ratio = encodeSavedPosition(
                currentOffset = currentPageOffset,
                fullTextLength = fullText.length
            )
            repository.update(book.copy(
                lastReadPosition = ratio,
                lastReadTime = System.currentTimeMillis()
            ))
            DebugLogger.d("ReaderVM", "进度已保存: offset=$currentPageOffset, ratio=$ratio")
        }
    }
}
