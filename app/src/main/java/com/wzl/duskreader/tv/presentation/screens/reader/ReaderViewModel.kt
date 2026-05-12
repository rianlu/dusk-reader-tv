package com.wzl.duskreader.tv.presentation.screens.reader

import android.util.Log
import android.util.LruCache
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.BookChapter
import com.wzl.duskreader.tv.data.reader.ChapterScanResult
import com.wzl.duskreader.tv.data.reader.EpubReaderEngine
import com.wzl.duskreader.tv.data.reader.TxtReaderEngine
import com.wzl.duskreader.tv.data.repositories.BookChapterRepository
import com.wzl.duskreader.tv.data.repositories.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 章节级阅读 ViewModel（业内主流商业 App 的标准架构）
 *
 * 核心策略：
 * - 打开书时**只扫描章节索引**（流式 IO），整本不进内存
 * - 进入 Reader 后**只加载当前章节**（5-50KB 量级），分页只针对该章节
 * - 翻到章节首/末按方向键 → 切换到上/下一章节
 * - 阅读进度存 (chapterIndex, charOffsetInChapter)，不受字号变化影响
 * - LRU 缓存最近 3 章节文本，前后翻章命中缓存秒切
 */
@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val repository: BookRepository,
    private val chapterRepository: BookChapterRepository,
    private val readerSettingsStore: ReaderSettingsStore,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    /** 暴露给 UI 的精简章节信息（不含字节偏移） */
    data class ReaderChapter(val index: Int, val title: String)

    private val bookId: Long =
        savedStateHandle.get<String>(ReaderScreen.BookIdBundleKey)?.toLongOrNull() ?: 0L

    private var book: Book? = null
    private var charset: Charset = Charsets.UTF_8
    private var txtEngine: TxtReaderEngine? = null
    private var epubEngine: EpubReaderEngine? = null
    private var bookChapters: List<BookChapter> = emptyList()

    private var currentChapterIndexValue: Int = 0
    private var currentChapterText: String = ""

    /** 当前章节内的字符偏移（从 0 开始），是阅读进度的一半 */
    private var currentCharOffsetInChapter: Int = 0

    /** LRU 缓存近 3 章节的文本，前后翻章不重复 IO */
    private val chapterTextCache = LruCache<Int, String>(3)

    private var progressSaveJob: Job? = null

    private val _readerSettings = MutableStateFlow(readerSettingsStore.read())
    val readerSettings = _readerSettings.asStateFlow()

    private val _bookTitle = MutableStateFlow("")
    val bookTitle = _bookTitle.asStateFlow()

    private val _pages = MutableStateFlow<List<ReaderPage>>(emptyList())
    val pages = _pages.asStateFlow()

    private val _isLoaded = MutableStateFlow(false)
    val isLoaded = _isLoaded.asStateFlow()

    private val _totalProgress = MutableStateFlow(0f)
    val totalProgress = _totalProgress.asStateFlow()

    private val _chapters = MutableStateFlow<List<ReaderChapter>>(emptyList())
    val chapters = _chapters.asStateFlow()

    private val _currentChapterTitle = MutableStateFlow("开始")
    val currentChapterTitle = _currentChapterTitle.asStateFlow()

    private val _currentChapterIndex = MutableStateFlow(0)
    val currentChapterIndex = _currentChapterIndex.asStateFlow()

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
                _pages.value = listOf(ReaderPage("书籍不存在", 0, 0))
                _isLoaded.value = true
                return@launch
            }
            book = loaded
            _bookTitle.value = loaded.title

            val file = File(loaded.path)
            if (!file.exists()) {
                _pages.value = listOf(ReaderPage("文件不存在：${loaded.path}", 0, 0))
                _isLoaded.value = true
                return@launch
            }

            val format = loaded.format.ifBlank { file.extension }.uppercase()
            if (format == FORMAT_EPUB || file.extension.equals("epub", ignoreCase = true)) {
                epubEngine = EpubReaderEngine(file)
                txtEngine = null
                Log.d(TAG, "EPUB 文件=${file.length()}字节")
            } else {
                val engine = TxtReaderEngine(file)
                txtEngine = engine
                epubEngine = null
                charset = engine.detectCharset()
                Log.d(TAG, "TXT 编码=$charset 文件=${file.length()}字节")
            }

            // 章节索引：先看数据库；没有就流式扫描
            var chapters = chapterRepository.getByBookId(bookId)
            if (epubEngine != null || chapters.isEmpty()) {
                val scanStart = System.currentTimeMillis()
                val scanResults = scanChapters()
                chapters = scanResults.mapIndexed { index, scan ->
                    val nextOffset = scanResults.getOrNull(index + 1)?.byteOffset ?: file.length()
                    BookChapter(
                        bookId = bookId,
                        chapterIndex = index,
                        title = scan.title,
                        byteOffset = scan.byteOffset,
                        byteLength = if (epubEngine != null) 0 else (nextOffset - scan.byteOffset).toInt(),
                    )
                }
                chapterRepository.replaceForBook(bookId, chapters)
                Log.d(
                    TAG,
                    "章节扫描完成 bookId=$bookId 章节数=${chapters.size} 耗时=${System.currentTimeMillis() - scanStart}ms",
                )
            } else {
                Log.d(TAG, "章节索引命中缓存 bookId=$bookId 章节数=${chapters.size}")
            }
            bookChapters = chapters
            _chapters.value = chapters.map { ReaderChapter(it.chapterIndex, it.title) }

            // 写回 Book.totalSize（章节级架构下复用为「总章节数」），用于书架和详情页显示进度
            val expectedTotal = chapters.size.toLong()
            val effectiveBook = if (loaded.totalSize != expectedTotal) {
                val updated = loaded.copy(totalSize = expectedTotal)
                repository.update(updated)
                updated
            } else {
                loaded
            }
            book = effectiveBook

            // 恢复阅读进度
            val maxIndex = (chapters.lastIndex).coerceAtLeast(0)
            currentChapterIndexValue = effectiveBook.lastReadChapter.coerceIn(0, maxIndex)
            currentCharOffsetInChapter = effectiveBook.lastReadPosition.coerceAtLeast(0)

            loadCurrentChapter()

            _isLoaded.value = true
            updateProgress()
            updateCurrentChapterTitle()
        }
    }

    private suspend fun loadCurrentChapter() {
        val ch = bookChapters.getOrNull(currentChapterIndexValue) ?: return
        val cached = chapterTextCache.get(ch.chapterIndex)
        currentChapterText = if (cached != null) {
            cached
        } else {
            withContext(Dispatchers.IO) {
                val raw = runCatching {
                    readChapterText(ch)
                }.getOrElse {
                    Log.e(TAG, "读取章节失败 idx=${ch.chapterIndex}", it)
                    ""
                }
                val formatted = formatChapterText(raw)
                chapterTextCache.put(ch.chapterIndex, formatted)
                formatted
            }
        }
        // 越界保护
        if (currentCharOffsetInChapter > currentChapterText.length) {
            currentCharOffsetInChapter = 0
        }
    }

    fun performPaging(
        textMeasurer: TextMeasurer,
        containerConstraints: Constraints,
        textStyle: TextStyle,
    ) {
        if (!_isLoaded.value || currentChapterText.isEmpty()) return
        if (_isPaging.value || !containerConstraints.hasBoundedWidth || !containerConstraints.hasBoundedHeight) return

        viewModelScope.launch(Dispatchers.Default) {
            _isPaging.value = true
            val startTime = System.currentTimeMillis()
            val pagingChapterIndex = currentChapterIndexValue
            val chapterText = currentChapterText
            try {
                val pageHeight = containerConstraints.maxHeight
                val unbounded = Constraints(
                    maxWidth = containerConstraints.maxWidth,
                    maxHeight = (pageHeight * PAGING_WINDOW_PAGES).coerceAtLeast(pageHeight),
                )
                val pages = mutableListOf<ReaderPage>()
                var textOffset = 0

                while (textOffset < chapterText.length) {
                    val windowText = chapterText.substring(textOffset)
                    val layoutResult: TextLayoutResult = try {
                        textMeasurer.measure(
                            text = windowText,
                            style = textStyle,
                            constraints = unbounded,
                            softWrap = true,
                        )
                    } catch (oom: OutOfMemoryError) {
                        Log.e(TAG, "measure 内存不足，跳过本次分页", oom)
                        return@launch
                    } catch (t: Throwable) {
                        Log.e(TAG, "measure 异常，跳过本次分页", t)
                        return@launch
                    }

                    val totalLines = layoutResult.lineCount
                    if (totalLines <= 0) break

                    var lineCursor = 0
                    var lastWindowEnd = 0
                    while (lineCursor < totalLines) {
                        val pageTop = layoutResult.getLineTop(lineCursor)
                        val pageBottomLimit = pageTop + pageHeight
                        var lastFitLine = lineCursor
                        var probe = lineCursor
                        while (probe < totalLines && layoutResult.getLineBottom(probe) <= pageBottomLimit) {
                            lastFitLine = probe
                            probe++
                        }
                        val pageStartChar = layoutResult.getLineStart(lineCursor)
                        val pageEndChar = layoutResult.getLineEnd(lastFitLine, visibleEnd = true)
                        if (pageEndChar <= lastWindowEnd) break

                        val absoluteStart = textOffset + pageStartChar
                        val absoluteEnd = textOffset + pageEndChar
                        pages += ReaderPage(
                            content = chapterText.substring(absoluteStart, absoluteEnd),
                            startOffset = absoluteStart,
                            endOffset = absoluteEnd,
                        )
                        lastWindowEnd = pageEndChar
                        lineCursor = lastFitLine + 1
                    }

                    if (lastWindowEnd <= 0) break
                    textOffset += lastWindowEnd
                }

                // 根据章节内字符偏移定位到对应页
                val targetIndex = if (pages.isEmpty()) {
                    null
                } else {
                    val found = pages.indexOfFirst { currentCharOffsetInChapter < it.endOffset }
                    if (found < 0) pages.lastIndex else found
                }

                if (pagingChapterIndex == currentChapterIndexValue && chapterText == currentChapterText) {
                    _pendingPageIndex.value = targetIndex
                    _pages.value = pages
                }
                Log.d(
                    TAG,
                    "章节分页 idx=$currentChapterIndexValue 页数=${pages.size} 耗时=${System.currentTimeMillis() - startTime}ms",
                )
            } finally {
                _isPaging.value = false
            }
        }
    }

    fun onPageChanged(pageIndex: Int) {
        val page = _pages.value.getOrNull(pageIndex) ?: return
        currentCharOffsetInChapter = page.startOffset
        updateProgress()
        saveProgress()
    }

    /** 翻到下一章节（在 ReaderScreen.moveForward 已是末页时调用） */
    fun loadNextChapter() {
        if (_isPaging.value) return
        if (currentChapterIndexValue >= bookChapters.lastIndex) return
        currentChapterIndexValue++
        currentCharOffsetInChapter = 0
        viewModelScope.launch {
            loadCurrentChapter()
            updateProgress()
            updateCurrentChapterTitle()
            requestPaging()
        }
    }

    /** 翻到上一章节（在 ReaderScreen.moveBackward 已是首页时调用），落点是上一章末页 */
    fun loadPreviousChapter() {
        if (_isPaging.value) return
        if (currentChapterIndexValue <= 0) return
        currentChapterIndexValue--
        viewModelScope.launch {
            loadCurrentChapter()
            // 落点设到章节末，performPaging 会算出末页并触发 pendingPageIndex
            currentCharOffsetInChapter = (currentChapterText.length - 1).coerceAtLeast(0)
            updateProgress()
            updateCurrentChapterTitle()
            requestPaging()
        }
    }

    /** 跳转到指定章节（目录点击使用） */
    fun jumpToChapter(chapterIndex: Int) {
        if (_isPaging.value) return
        val target = chapterIndex.coerceIn(0, bookChapters.lastIndex.coerceAtLeast(0))
        currentChapterIndexValue = target
        currentCharOffsetInChapter = 0
        viewModelScope.launch {
            loadCurrentChapter()
            updateProgress()
            updateCurrentChapterTitle()
            requestPaging()
        }
    }

    /** 字号 / 行间距变化时调用，仅重排当前章节 */
    fun repaginate() {
        if (!_isLoaded.value) return
        requestPaging()
    }

    fun consumePendingPageIndex() {
        _pendingPageIndex.value = null
    }

    fun updateFontSize(fontSize: Int) {
        updateReaderSettings { it.copy(fontSize = fontSize.coerceIn(18, 80)) }
    }

    fun updateTheme(theme: ReaderTheme) {
        updateReaderSettings { it.copy(theme = theme) }
    }

    fun updateTextBrightness(brightness: ReaderTextBrightness) {
        updateReaderSettings { it.copy(textBrightness = brightness) }
    }

    fun updateLineSpacing(lineSpacing: Float) {
        updateReaderSettings { it.copy(lineSpacing = lineSpacing.coerceIn(1.3f, 2.4f)) }
    }

    fun updatePageTurnMode(mode: PageTurnMode) {
        updateReaderSettings { it.copy(pageTurnMode = mode) }
    }

    fun updateAutoTurnSeconds(seconds: Int) {
        updateReaderSettings {
            it.copy(
                autoTurnSeconds = seconds.coerceIn(
                    AutoTurnInterval.MIN_SECONDS,
                    AutoTurnInterval.MAX_SECONDS,
                ),
            )
        }
    }

    fun saveProgress() {
        progressSaveJob?.cancel()
        progressSaveJob = viewModelScope.launch { persistProgress() }
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
        repository.update(
            bk.copy(
                lastReadChapter = currentChapterIndexValue,
                lastReadPosition = currentCharOffsetInChapter,
                lastReadTime = System.currentTimeMillis(),
            ),
        )
    }

    private fun requestPaging() {
        _pendingPageIndex.value = null
        _pagingRequestVersion.value += 1
    }

    private fun scanChapters() = epubEngine?.scanChapters()
        ?: txtEngine?.scanChapters(charset)
        ?: listOf(ChapterScanResult("正文", 0L))

    private fun readChapterText(chapter: BookChapter): String {
        return epubEngine?.readChapterText(chapter.byteOffset.toInt())
            ?: txtEngine?.readChapterText(chapter.byteOffset, chapter.byteLength, charset)
            ?: ""
    }

    private fun updateReaderSettings(transform: (ReaderSettings) -> ReaderSettings) {
        _readerSettings.update { current ->
            transform(current).also(readerSettingsStore::save)
        }
    }

    private fun updateProgress() {
        val total = bookChapters.size.coerceAtLeast(1)
        val chapterRatio = currentChapterIndexValue.toFloat() / total
        val withinChapter = if (currentChapterText.isNotEmpty()) {
            (currentCharOffsetInChapter.toFloat() / currentChapterText.length) / total
        } else {
            0f
        }
        _totalProgress.value = (chapterRatio + withinChapter).coerceIn(0f, 1f)
    }

    private fun updateCurrentChapterTitle() {
        _currentChapterIndex.value = currentChapterIndexValue
        _currentChapterTitle.value = bookChapters.getOrNull(currentChapterIndexValue)?.title ?: "开始"
    }

    companion object {
        private const val TAG = "ReaderVM"
        private const val PAGING_WINDOW_PAGES = 80
        private const val FORMAT_EPUB = "EPUB"
    }
}

/** 章节文本格式化：每段首加全角空格、段间空行 */
internal fun formatChapterText(raw: String): String {
    if (raw.isEmpty()) return ""
    return raw.split("\n")
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .joinToString(separator = "\n\n") { "　　$it" }
}
