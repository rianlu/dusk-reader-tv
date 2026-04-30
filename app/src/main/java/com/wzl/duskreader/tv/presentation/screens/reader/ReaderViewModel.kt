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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: BookRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    data class Chapter(val title: String, val offset: Int)

    private enum class PagingAnchor {
        START,
        END,
    }

    private val bookId: Long = savedStateHandle.get<String>(ReaderScreen.BookIdBundleKey)?.toLongOrNull() ?: 0L

    private var book: Book? = null
    private var contentParagraphs: List<String> = emptyList()
    private var fullText: String = ""
    private var windowStartOffset: Int = 0
    private var currentPageOffset: Int = 0
    private var pendingPagingAnchor = PagingAnchor.START
    private var progressSaveJob: Job? = null

    private val _bookTitle = MutableStateFlow("")
    val bookTitle = _bookTitle.asStateFlow()

    private val _pages = MutableStateFlow<List<ReaderPage>>(emptyList())
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

    private val _pagingRequestVersion = MutableStateFlow(0)
    val pagingRequestVersion = _pagingRequestVersion.asStateFlow()

    private val _pendingPageIndex = MutableStateFlow<Int?>(null)
    val pendingPageIndex = _pendingPageIndex.asStateFlow()

    init {
        loadBook()
    }

    private fun loadBook() {
        viewModelScope.launch(Dispatchers.IO) {
            val loaded = repository.getBookById(bookId)
            if (loaded == null) {
                _pages.value = listOf(ReaderPage(content = "书籍不存在", startOffset = 0, endOffset = 0))
                _isLoaded.value = true
                return@launch
            }
            book = loaded
            _bookTitle.value = loaded.title

            val file = File(loaded.path)
            if (!file.exists()) {
                _pages.value = listOf(
                    ReaderPage(content = "文件不存在：${loaded.path}", startOffset = 0, endOffset = 0),
                )
                _isLoaded.value = true
                return@launch
            }

            val rawText = TxtReaderEngine(file).readFullContent()
            contentParagraphs = rawText
                .split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            rebuildFullText(DEFAULT_PARAGRAPH_SPACING_DP, preserveProgress = false)

            if (fullText.isEmpty()) {
                _pages.value = listOf(ReaderPage(content = "暂无可读取内容", startOffset = 0, endOffset = 0))
                _isLoaded.value = true
                return@launch
            }

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

    private fun rebuildFullText(paragraphSpacing: Int, preserveProgress: Boolean) {
        val previousLength = fullText.length
        fullText = buildFormattedReaderText(contentParagraphs, paragraphSpacing)

        currentPageOffset = if (preserveProgress) {
            remapOffsetByProgress(
                currentOffset = currentPageOffset,
                previousLength = previousLength,
                newLength = fullText.length,
            )
        } else {
            0
        }

        scanChapters()
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
        _totalProgress.value = progressRatioForOffset(
            currentOffset = currentPageOffset,
            fullTextLength = fullText.length,
        )
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
        requestPaging(anchor = PagingAnchor.START)
    }

    fun performPaging(
        textMeasurer: TextMeasurer,
        containerConstraints: Constraints,
        textStyle: TextStyle,
    ) {
        if (!_isLoaded.value || fullText.isEmpty()) return
        if (_isPaging.value || !containerConstraints.hasBoundedWidth || !containerConstraints.hasBoundedHeight) return

        viewModelScope.launch(Dispatchers.Default) {
            _isPaging.value = true
            val startTime = System.currentTimeMillis()
            try {
                delay(100)

                val totalLength = fullText.length
                val windowEndOffset = (windowStartOffset + WINDOW_LIMIT_CHARS).coerceAtMost(totalLength)
                val windowPages = mutableListOf<ReaderPage>()
                var pageStartOffset = windowStartOffset

                while (pageStartOffset < windowEndOffset && windowPages.size < MAX_WINDOW_PAGES) {
                    val remainingText = fullText.substring(pageStartOffset, windowEndOffset)
                    val layoutResult: TextLayoutResult = textMeasurer.measure(
                        text = remainingText,
                        style = textStyle,
                        constraints = containerConstraints,
                        softWrap = true,
                    )
                    val measuredBreakIndex = if (layoutResult.hasVisualOverflow) {
                        layoutResult.getLineEnd(layoutResult.lineCount - 1, true)
                    } else {
                        remainingText.length
                    }
                    val safeBreakIndex = measuredBreakIndex.coerceAtLeast(1)
                    val pageText = remainingText.substring(0, safeBreakIndex)
                    val pageEndOffset = (pageStartOffset + pageText.length).coerceAtMost(windowEndOffset)
                    windowPages += ReaderPage(
                        content = pageText,
                        startOffset = pageStartOffset,
                        endOffset = pageEndOffset,
                    )
                    pageStartOffset = pageEndOffset
                }

                _pages.value = windowPages
                _pendingPageIndex.value = when (pendingPagingAnchor) {
                    PagingAnchor.START -> 0
                    PagingAnchor.END -> windowPages.lastIndex
                }.takeIf { windowPages.isNotEmpty() }
                Log.d(
                    TAG,
                    "窗口分页完成 [${windowStartOffset}..${windowPages.lastOrNull()?.endOffset ?: windowStartOffset}], " +
                        "页数=${windowPages.size}, 耗时=${System.currentTimeMillis() - startTime}ms",
                )
            } finally {
                _isPaging.value = false
            }
        }
    }

    fun onPageChanged(pageIndex: Int) {
        val page = _pages.value.getOrNull(pageIndex) ?: return
        currentPageOffset = page.startOffset
        updateProgress()
        saveProgress()
    }

    fun loadNextWindow() {
        if (_isPaging.value) return
        val lastPage = _pages.value.lastOrNull() ?: return
        if (lastPage.endOffset >= fullText.length) return
        windowStartOffset = lastPage.endOffset
        currentPageOffset = windowStartOffset
        updateProgress()
        requestPaging(anchor = PagingAnchor.START)
    }

    fun loadPreviousWindow() {
        if (_isPaging.value || windowStartOffset <= 0) return
        windowStartOffset = (windowStartOffset - PREVIOUS_WINDOW_STEP_CHARS).coerceAtLeast(0)
        currentPageOffset = windowStartOffset
        updateProgress()
        requestPaging(anchor = PagingAnchor.END)
    }

    fun repaginateFromCurrentPosition(paragraphSpacing: Int) {
        if (!_isLoaded.value || contentParagraphs.isEmpty()) return
        rebuildFullText(paragraphSpacing, preserveProgress = true)
        windowStartOffset = currentPageOffset
        updateProgress()
        requestPaging(anchor = PagingAnchor.START)
    }

    fun consumePendingPageIndex() {
        _pendingPageIndex.value = null
    }

    fun saveProgress() {
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            persistProgress()
        }
    }

    fun saveProgressBeforeExit(onComplete: () -> Unit) {
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch {
            runCatching { persistProgress() }
                .onFailure { Log.e(TAG, "退出前保存进度失败", it) }
            onComplete()
        }
    }

    override fun onCleared() {
        progressSaveJob?.cancel()
        super.onCleared()
    }

    private suspend fun persistProgress() = withContext(Dispatchers.IO) {
        val bk = book ?: return@withContext
        val ratio = encodeSavedPosition(currentPageOffset, fullText.length)
        repository.update(
            bk.copy(
                lastReadPosition = ratio,
                lastReadTime = System.currentTimeMillis(),
            ),
        )
        Log.d(TAG, "进度已保存：offset=$currentPageOffset, ratio=$ratio")
    }

    private fun requestPaging(anchor: PagingAnchor) {
        pendingPagingAnchor = anchor
        _pages.value = emptyList()
        _pendingPageIndex.value = null
        _pagingRequestVersion.value += 1
    }

    companion object {
        private const val TAG = "ReaderVM"
        private const val DEFAULT_PARAGRAPH_SPACING_DP = 16
        private const val WINDOW_LIMIT_CHARS = 50_000
        private const val PREVIOUS_WINDOW_STEP_CHARS = 35_000
        private const val MAX_WINDOW_PAGES = 200
    }
}

internal fun buildFormattedReaderText(paragraphs: List<String>, paragraphSpacing: Int): String {
    if (paragraphs.isEmpty()) return ""
    val separator = paragraphSeparatorForSpacing(paragraphSpacing)
    return paragraphs.joinToString(separator = separator) { "　　$it" }
}

internal fun paragraphSeparatorForSpacing(paragraphSpacing: Int): String {
    val breakCount = when {
        paragraphSpacing <= 16 -> 2
        paragraphSpacing <= 24 -> 3
        else -> 4
    }
    return "\n".repeat(breakCount)
}

internal fun progressRatioForOffset(currentOffset: Int, fullTextLength: Int): Float {
    if (fullTextLength <= 0) return 0f
    return currentOffset.toFloat()
        .div(fullTextLength)
        .coerceIn(0f, 1f)
}

internal fun remapOffsetByProgress(
    currentOffset: Int,
    previousLength: Int,
    newLength: Int,
): Int {
    if (newLength <= 0) return 0
    val progressRatio = progressRatioForOffset(
        currentOffset = currentOffset,
        fullTextLength = previousLength,
    )
    return (progressRatio * newLength)
        .toInt()
        .coerceIn(0, newLength)
}
