package com.wzl.duskreader.tv.presentation.screens.reader

import android.util.Log
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.reader.TxtReaderEngine
import com.wzl.duskreader.tv.data.reader.decodeSavedPosition
import com.wzl.duskreader.tv.data.reader.encodeSavedPosition
import com.wzl.duskreader.tv.data.repositories.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: BookRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    data class Chapter(val title: String, val offset: Int)

    private val bookId: Long = savedStateHandle.get<String>(ReaderScreen.BookIdBundleKey)?.toLongOrNull() ?: 0L

    private var book: Book? = null
    private var fullText: String = ""
    private var windowStartOffset: Int = 0
    private var currentPageOffset: Int = 0
    private var pageLengths = mutableListOf<Int>()

    private val _bookTitle = MutableStateFlow("")
    val bookTitle = _bookTitle.asStateFlow()

    private val _pages = MutableStateFlow<List<String>>(emptyList())
    val pages = _pages.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    private val _totalProgress = MutableStateFlow(0f)
    val totalProgress = _totalProgress.asStateFlow()

    private val _chapters = MutableStateFlow<List<Chapter>>(emptyList())
    val chapters = _chapters.asStateFlow()

    private val _currentChapterTitle = MutableStateFlow("开始")
    val currentChapterTitle = _currentChapterTitle.asStateFlow()

    private val _isPaging = MutableStateFlow(false)
    val isPaging = _isPaging.asStateFlow()

    private val _snapToEndAfterPaging = MutableStateFlow(false)
    val snapToEndAfterPaging = _snapToEndAfterPaging.asStateFlow()

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = repository.getBookById(bookId)
            if (loaded == null) {
                _pages.value = listOf("书籍不存在")
                _isLoaded.value = true
                return@launch
            }
            book = loaded
            _bookTitle.value = loaded.title

            val file = File(loaded.path)
            if (!file.exists()) {
                _pages.value = listOf("文件不存在：${loaded.path}")
                _isLoaded.value = true
                return@launch
            }

            val rawText = TxtReaderEngine(file).readFullContent()
            fullText = rawText
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .joinToString("\n\n") { "　　$it" }

            scanChapters()

            currentPageOffset = decodeSavedPosition(
                savedPos = loaded.lastReadPosition,
                fullTextLength = fullText.length,
            )
            windowStartOffset = currentPageOffset

            _isLoaded.value = true
            updateProgress()
            updateCurrentChapterTitle()
            Log.d(TAG, "文本加载完成，长度=${fullText.length}, 初始偏移=$currentPageOffset")
        }
    }

    private fun scanChapters() {
        val regex = Regex("^\\s*(第.{1,9}[章节回部集卷]).*", RegexOption.MULTILINE)
        val found = regex.findAll(fullText)
            .map { match -> Chapter(match.groupValues[0].trim().take(20), match.range.first) }
            .toMutableList()
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
        val chapter = _chapters.value.lastOrNull { it.offset <= currentPageOffset }
            ?: _chapters.value.firstOrNull()
        if (chapter != null) {
            _currentChapterTitle.value = chapter.title
        }
    }

    fun jumpToOffset(offset: Int) {
        val target = offset.coerceIn(0, fullText.length)
        currentPageOffset = target
        windowStartOffset = target
        updateProgress()
        _pages.value = emptyList()
    }

    fun performPaging(
        textMeasurer: TextMeasurer,
        containerConstraints: Constraints,
        textStyle: TextStyle,
    ) {
        if (!_isLoaded.value || fullText.isEmpty()) return
        if (_isPaging.value) return

        viewModelScope.launch(Dispatchers.Default) {
            _isPaging.value = true
            val startTime = System.currentTimeMillis()
            try {
                delay(100)

                val pagesList = mutableListOf<String>()
                val lengths = mutableListOf<Int>()

                val windowLimit = 40_000
                val totalLength = fullText.length
                val endOfWindow = (windowStartOffset + windowLimit).coerceAtMost(totalLength)
                var remainingText = fullText.substring(windowStartOffset, endOfWindow)

                while (remainingText.isNotEmpty() && pagesList.size < 100) {
                    val layoutResult: TextLayoutResult = textMeasurer.measure(
                        text = remainingText,
                        style = textStyle,
                        constraints = containerConstraints,
                        softWrap = true,
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
                Log.d(
                    TAG,
                    "窗口分页完成 [${windowStartOffset}..${windowStartOffset + lengths.sum()}], 耗时=${System.currentTimeMillis() - startTime}ms",
                )
            } finally {
                _isPaging.value = false
            }
        }
    }

    fun onPageChanged(pageIndex: Int) {
        if (pageIndex < 0 || pageIndex >= pageLengths.size) return
        var offsetInWindow = 0
        for (i in 0 until pageIndex) {
            offsetInWindow += pageLengths[i]
        }
        currentPageOffset = windowStartOffset + offsetInWindow
        updateProgress()
    }

    fun loadNextWindow() {
        if (_isPaging.value || pageLengths.isEmpty()) return
        windowStartOffset += pageLengths.sum()
        currentPageOffset = windowStartOffset
        _snapToEndAfterPaging.value = false
        _pages.value = emptyList()
    }

    fun loadPreviousWindow() {
        if (_isPaging.value) return
        windowStartOffset = (windowStartOffset - 35_000).coerceAtLeast(0)
        currentPageOffset = windowStartOffset
        _snapToEndAfterPaging.value = true
        _pages.value = emptyList()
    }

    fun consumeSnapToEndFlag() {
        _snapToEndAfterPaging.value = false
    }

    fun saveProgress() {
        val bk = book ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val ratio = encodeSavedPosition(currentPageOffset, fullText.length)
            repository.update(
                bk.copy(
                    lastReadPosition = ratio,
                    lastReadTime = System.currentTimeMillis(),
                ),
            )
            Log.d(TAG, "进度已保存：offset=$currentPageOffset, ratio=$ratio")
        }
    }

    override fun onCleared() {
        saveProgress()
        super.onCleared()
    }

    companion object {
        private const val TAG = "ReaderVM"
    }
}
