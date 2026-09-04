@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.wzl.duskreader.tv.presentation.screens.bookshelf

import android.view.KeyEvent
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Carousel
import androidx.tv.material3.CarouselDefaults
import androidx.tv.material3.CarouselState
import androidx.tv.material3.FilterChip
import androidx.tv.material3.FilterChipDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.BookList
import com.wzl.duskreader.tv.data.entities.BookKind
import com.wzl.duskreader.tv.data.entities.kind
import com.wzl.duskreader.tv.data.entities.hasReadingHistory
import com.wzl.duskreader.tv.data.entities.progressRatio
import com.wzl.duskreader.tv.presentation.common.BookCover
import com.wzl.duskreader.tv.presentation.common.BooksGrid
import com.wzl.duskreader.tv.presentation.common.DuskTvButton
import com.wzl.duskreader.tv.presentation.common.DuskTvButtonStyle
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding
import com.wzl.duskreader.tv.presentation.utils.requestFocusSafely
import com.wzl.duskreader.tv.tvmaterial.StandardDialog

private const val HOME_TOP_BAR_HIDE_THRESHOLD_PX = 300
private const val CAROUSEL_ITEM_COUNT = 5
private const val CAROUSEL_FADE_MS = 400
private const val LIBRARY_TOP_BAR_HIDE_THRESHOLD_PX = 100
private val BOOK_POSTER_ASPECT_RATIO = 3f / 4f

// 轮播位置持久化（官方 JetStream CarouselSaver 模式，进程重建恢复轮播页）
private val CarouselSaver = Saver<CarouselState, Int>(
    save = { it.activeItemIndex },
    restore = { CarouselState(it) },
)

enum class BookshelfScreenMode {
    Home,
    Library,
}

@Composable
fun BookshelfScreen(
    onBookClick: (book: Book) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    mode: BookshelfScreenMode = BookshelfScreenMode.Home,
    viewModel: BookshelfScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val liveSearchQuery by viewModel.liveSearchQuery.collectAsStateWithLifecycle()

    DuskPageBackground {
        when (val state = uiState) {
            is BookshelfUiState.Loading -> Unit
            is BookshelfUiState.Ready -> {
                if (state.allBooks.isEmpty()) {
                    LaunchedEffect(mode) { onScroll(true) }
                    EmptyBookshelf(mode = mode)
                } else {
                    when (mode) {
                        BookshelfScreenMode.Home -> HomeBookshelf(
                            recentBooks = state.recentBooks,
                            allBooks = state.allBooks,
                            onBookClick = onBookClick,
                            onScroll = onScroll,
                            isTopBarVisible = isTopBarVisible,
                        )

                        BookshelfScreenMode.Library -> LibraryBookshelf(
                            allBooks = state.allBooks,
                            libraryBooks = state.libraryBooks,
                            searchQuery = state.searchQuery,
                            liveSearchQuery = liveSearchQuery,
                            formatFilter = state.formatFilter,
                            librarySort = state.librarySort,
                            onBookClick = onBookClick,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            onSelectFormatFilter = viewModel::setFormatFilter,
                            onSelectLibrarySort = viewModel::setLibrarySort,
                            onScroll = onScroll,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeBookshelf(
    recentBooks: BookList,
    allBooks: BookList,
    onBookClick: (book: Book) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
) {
    val childPadding = rememberChildPadding()
    val listState = rememberLazyListState()
    val startRequester = remember { FocusRequester() }
    // 轮播数据：最近阅读优先，不足补全量书库（上限 5 本，与网格首行等量）
    val carouselBooks = remember(recentBooks, allBooks) {
        (recentBooks.ifEmpty { allBooks }).take(CAROUSEL_ITEM_COUNT)
    }

    val shouldShowTopBar by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex == 0 &&
                listState.firstVisibleItemScrollOffset < HOME_TOP_BAR_HIDE_THRESHOLD_PX
        }
    }
    LaunchedEffect(shouldShowTopBar) { onScroll(shouldShowTopBar) }
    LaunchedEffect(isTopBarVisible) {
        if (isTopBarVisible) listState.animateScrollToItem(0)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = childPadding.start,
            end = childPadding.end,
            top = 34.dp,
            bottom = 108.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item {
            ContinueReadingCarousel(
                books = carouselBooks,
                totalCount = allBooks.size,
                hasRecentBook = recentBooks.isNotEmpty(),
                startRequester = startRequester,
                onBookClick = onBookClick,
            )
        }
    }
}

@Composable
private fun LibraryBookshelf(
    allBooks: BookList,
    libraryBooks: BookList,
    searchQuery: String,
    liveSearchQuery: String,
    formatFilter: LibraryFormatFilter,
    librarySort: LibrarySort,
    onBookClick: (book: Book) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onSelectFormatFilter: (LibraryFormatFilter) -> Unit,
    onSelectLibrarySort: (LibrarySort) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
) {
    val childPadding = rememberChildPadding()
    val gridState = rememberLazyGridState()
    val searchRequester = remember { FocusRequester() }
    var showSearchDialog by rememberSaveable { mutableStateOf(false) }
    val firstChipRequester = remember { FocusRequester() }

    val shouldShowTopBar by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 &&
                gridState.firstVisibleItemScrollOffset < LIBRARY_TOP_BAR_HIDE_THRESHOLD_PX
        }
    }
    // 顶栏显隐纯滚动驱动（对齐官方 HomeScreen 模式）：不掺入焦点状态，
    // 否则网格聚焦→顶栏收起→拉回顶部与 pivot 滚动互相打架，产生布局抖动/跳行
    LaunchedEffect(shouldShowTopBar) { onScroll(shouldShowTopBar) }
    // 空结果时聚焦搜索入口（恒组合，安全）；对话框弹出时不抢焦点
    LaunchedEffect(libraryBooks.isEmpty(), showSearchDialog) {
        if (libraryBooks.isEmpty() && !showSearchDialog) searchRequester.requestFocusSafely()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LibraryToolbar(
            books = allBooks,
            shownCount = libraryBooks.size,
            searchQuery = searchQuery,
            formatFilter = formatFilter,
            librarySort = librarySort,
            searchRequester = searchRequester,
            firstChipRequester = firstChipRequester,
            hasResults = libraryBooks.isNotEmpty(),
            onSelectFormatFilter = onSelectFormatFilter,
            onSelectLibrarySort = onSelectLibrarySort,
            onSearchClick = { showSearchDialog = true },
            modifier = Modifier.padding(
                start = childPadding.start,
                end = childPadding.end,
                top = 8.dp,
                bottom = 8.dp,
            ),
        )
        if (libraryBooks.isEmpty()) {
            EmptyLibraryResults(
                hasSearch = searchQuery.isNotBlank(),
                hasFormatFilter = formatFilter != LibraryFormatFilter.All,
                modifier = Modifier.padding(
                    start = childPadding.start,
                    end = childPadding.end,
                    top = 56.dp,
                ),
            )
            return@Column
        }
        // 网格焦点治理已内收到 BooksGrid 容器（P1-1，对齐官方 MoviesRow 容器级焦点模式）：
        // pivot 滚动/restorer 恒组合锚点/边界最小规则全部由组件提供
        BooksGrid(
            books = libraryBooks,
            anchorRequester = searchRequester,
            onBookClick = onBookClick,
            modifier = Modifier.fillMaxSize(),
            state = gridState,
            upRequester = firstChipRequester,
            contentPadding = PaddingValues(
                start = childPadding.start,
                top = 10.dp,
                end = childPadding.end,
                bottom = 132.dp,
            ),
            bookTile = { book, tileModifier ->
                LibraryBookTile(
                    book = book,
                    modifier = tileModifier,
                    onClick = { onBookClick(book) },
                )
            },
        )
    }

    LibrarySearchDialog(
        showDialog = showSearchDialog,
        query = liveSearchQuery,
        onQueryChange = onSearchQueryChange,
        onDismissRequest = { showSearchDialog = false },
    )
}

/**
 * 首页轮播 Hero（官方 JetStream FeaturedMoviesCarousel 模式）：
 * 最近阅读的书横向轮播，D-pad 左右切换（Carousel 内建），选中键直达阅读。
 * 状态经 CarouselSaver 持久化（进程重建恢复轮播位置）。
 */
@Composable
private fun ContinueReadingCarousel(
    books: List<Book>,
    totalCount: Int,
    hasRecentBook: Boolean,
    startRequester: FocusRequester,
    onBookClick: (book: Book) -> Unit,
) {
    val carouselState = rememberSaveable(saver = CarouselSaver) { CarouselState(0) }

    Carousel(
        modifier = Modifier.fillMaxWidth(),
        itemCount = books.size,
        carouselState = carouselState,
        carouselIndicator = {
            CarouselDefaults.IndicatorRow(
                itemCount = books.size,
                activeItemIndex = carouselState.activeItemIndex,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 4.dp),
            )
        },
        contentTransformStartToEnd = fadeIn(tween(CAROUSEL_FADE_MS)) togetherWith fadeOut(tween(CAROUSEL_FADE_MS)),
        contentTransformEndToStart = fadeIn(tween(CAROUSEL_FADE_MS)) togetherWith fadeOut(tween(CAROUSEL_FADE_MS)),
    ) { index ->
        val book = books[index]
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(34.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCoverWithBadges(
                book = book,
                modifier = Modifier
                    .width(210.dp)
                    .aspectRatio(BOOK_POSTER_ASPECT_RATIO),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = if (hasRecentBook) "最近阅读" else "开始第一本书",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.58f),
                )
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                BookMetaChips(book = book)
                Text(
                    text = continueSubtitle(book, totalCount),
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                ReadingProgressBar(
                    progress = if (book.hasReadingHistory()) book.progressRatio() else 0f,
                    modifier = Modifier.widthIn(min = 360.dp, max = 560.dp),
                )
                Row(modifier = Modifier.focusGroup()) {
                    DuskTvButton(
                        text = if (book.hasReadingHistory()) "继续阅读" else "开始阅读",
                        icon = Icons.Outlined.AutoStories,
                        modifier = Modifier
                            .focusRequester(startRequester)
                            // 轮播边界封口：Hero 区左右不再外溢（由 Carousel 内建切换承接）
                            .focusProperties {
                                left = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                            },
                        onClick = { onBookClick(book) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryToolbar(
    books: BookList,
    shownCount: Int,
    searchQuery: String,
    formatFilter: LibraryFormatFilter,
    librarySort: LibrarySort,
    searchRequester: FocusRequester,
    firstChipRequester: FocusRequester,
    hasResults: Boolean,
    onSelectFormatFilter: (LibraryFormatFilter) -> Unit,
    onSelectLibrarySort: (LibrarySort) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (txtCount, epubCount) = remember(books) {
        books.count { it.kind() == BookKind.Novel } to books.count { it.kind() == BookKind.Epub }
    }
    val searchLabel = if (searchQuery.isBlank()) "搜索书名或作者" else "搜索: ${searchQuery.take(12)}"

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DuskTvButton(
                text = searchLabel,
                icon = Icons.Outlined.Search,
                style = DuskTvButtonStyle.Secondary,
                onClick = onSearchClick,
                modifier = Modifier
                    .focusRequester(searchRequester)
                    .focusProperties {
                        // 右邻与下邻交给默认 2D 焦点搜索（chip 行/网格都在几何下方）
                        if (!hasResults) down = FocusRequester.Cancel
                    },
            )
            // 筛选/排序平铺 chip 行（官方 MovieFilterChipRow 模式）：
            // 即点即生效，取代「按钮 + 弹窗」两层交互
            Row(
                modifier = Modifier.focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LibraryFormatFilter.entries.forEachIndexed { index, filter ->
                    LibraryFilterChip(
                        label = filter.label,
                        selected = filter == formatFilter,
                        onClick = { onSelectFormatFilter(filter) },
                        modifier = Modifier
                            .then(
                                if (index == 0) Modifier.focusRequester(firstChipRequester)
                                else Modifier,
                            )
                            .focusProperties {
                                if (!hasResults) down = FocusRequester.Cancel
                            },
                    )
                }
                // 分隔与排序组
                LibrarySort.entries.forEach { sort ->
                    LibraryFilterChip(
                        label = sort.label,
                        selected = sort == librarySort,
                        onClick = { onSelectLibrarySort(sort) },
                        modifier = Modifier.focusProperties {
                            if (!hasResults) down = FocusRequester.Cancel
                        },
                    )
                }
            }
        }
        Text(
            text = "共 ${books.size} 本 · TXT $txtCount · EPUB $epubCount · 当前显示 $shownCount 本",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
            color = Color.White.copy(alpha = 0.58f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * 书库筛选 chip：tv-material3 FilterChip + 暮阅聚焦签名（白底反相 + 2dp 描边，DESIGN.md §2.2/§4）。
 * 选中态 = 0.14 白填充 + ✓ 前缀（与阅读设置 OptionCard 一致）。
 */
@Composable
private fun LibraryFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val chipShape = MaterialTheme.shapes.small
    FilterChip(
        modifier = modifier.onFocusChanged { focused = it.isFocused || it.hasFocus },
        onClick = onClick,
        selected = selected,
        shape = FilterChipDefaults.shape(shape = chipShape),
        scale = FilterChipDefaults.scale(focusedScale = 1f),
        colors = FilterChipDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.05f),
            contentColor = Color.White.copy(alpha = 0.86f),
            selectedContainerColor = Color.White.copy(alpha = 0.14f),
            selectedContentColor = Color.White,
            focusedContainerColor = Color.White,
            focusedContentColor = Color.Black,
            focusedSelectedContainerColor = Color.White,
            focusedSelectedContentColor = Color.Black,
        ),
        border = FilterChipDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)),
                shape = chipShape,
            ),
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = chipShape,
            ),
        ),
    ) {
        Text(
            text = if (selected) "✓ $label" else label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun EmptyLibraryResults(
    hasSearch: Boolean,
    hasFormatFilter: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "没有找到匹配的书籍",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.86f),
        )
        Text(
            text = if (hasSearch || hasFormatFilter) "请调整搜索内容或格式筛选。" else "书库暂无可显示内容。",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.58f),
        )
    }
}

@Composable
private fun LibrarySearchDialog(
    showDialog: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val inputRequester = remember { FocusRequester() }
    val confirmRequester = remember { FocusRequester() }
    var inputFocused by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(showDialog) {
        // 弹窗首焦给「完成」按钮而非输入框：TV 输入框聚焦即弹 IME，
        // Gboard 会拦截全部 D-pad 方向键（实测复现），焦点会被钉死在输入框。
        // dialogFocusable 在组合期把焦点给第一个可聚焦子项（BasicTextField），
        // 这里等一帧再改判到完成按钮。
        if (showDialog) {
            withFrameNanos { }
            confirmRequester.requestFocusSafely()
        }
    }

    StandardDialog(
        showDialog = showDialog,
        onDismissRequest = onDismissRequest,
        title = { Text("搜索书库") },
        text = {
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(inputRequester)
                    .onFocusChanged { state ->
                        inputFocused = state.hasFocus
                        // 输入框聚焦/失焦都收起 IME：Gboard 一旦弹出就拦截全部
                        // D-pad 方向键（实测复现,焦点钉死在输入框,DOWN 逃逸失效）。
                        // TV 搜索词主要来自遥控器数字键/语音;物理键盘输入不受影响,
                        // 需要软键盘时可点按输入框区域唤出。
                        keyboardController?.hide()
                    }
                    .focusProperties {
                        left = FocusRequester.Cancel
                        right = FocusRequester.Cancel
                        // DOWN/UP 显式定向：IME 拦截时 Compose 收不到按键，
                        // 但部分输入法会放行方向键，此时定向到完成按钮
                        down = confirmRequester
                    }
                    .onPreviewKeyEvent { event ->
                        when (event.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN,
                            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN -> {
                                if (event.type == KeyEventType.KeyDown) {
                                    // 收起 IME 并把焦点交给完成按钮（双保险：
                                    // focusProperties.down 覆盖放行方向键的输入法，
                                    // 这里覆盖被 Compose 收到的方向键）
                                    keyboardController?.hide()
                                    confirmRequester.requestFocusSafely()
                                }
                                true
                            }

                            KeyEvent.KEYCODE_BACK -> {
                                if (event.type == KeyEventType.KeyUp) onDismissRequest()
                                true
                            }

                            else -> false
                        }
                    }
                    .border(
                        width = 2.dp,
                        color = if (inputFocused) Color.White else Color.White.copy(alpha = 0.18f),
                        shape = MaterialTheme.shapes.large,
                    )
                    .background(Color.White.copy(alpha = 0.08f), MaterialTheme.shapes.large)
                    .padding(horizontal = 18.dp, vertical = 15.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.titleMedium.copy(color = Color.White),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onDismissRequest() }),
                decorationBox = { innerTextField ->
                    Box {
                        if (query.isBlank()) {
                            Text(
                                text = "输入书名或作者",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.42f),
                            )
                        }
                        innerTextField()
                    }
                },
            )
        },
        confirmButton = {
            DuskTvButton(
                text = "完成",
                onClick = onDismissRequest,
                modifier = Modifier
                    .focusRequester(confirmRequester)
                    .focusProperties { up = inputRequester },
            )
        },
        dismissButton = if (query.isNotBlank()) {
            {
                DuskTvButton(
                    text = "清除",
                    style = DuskTvButtonStyle.Secondary,
                    onClick = { onQueryChange("") },
                )
            }
        } else {
            null
        },
    )
}


@Composable
private fun LibraryBookTile(
    book: Book,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val contentColor = if (focused) Color.Black else Color.White
    Surface(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { focused = it.hasFocus },
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.White.copy(alpha = 0.045f),
            focusedContainerColor = Color.White,
        ),
        border = ClickableSurfaceDefaults.border(
            border = Border(
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
                shape = MaterialTheme.shapes.large,
            ),
            // 2dp 聚焦白描边（DESIGN.md §2.2 全局签名，与阅读页/按钮/设置行统一）
            focusedBorder = Border(
                border = BorderStroke(2.dp, Color.White),
                shape = MaterialTheme.shapes.large,
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            BookCoverWithBadges(
                book = book,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(BOOK_POSTER_ASPECT_RATIO),
            )
            Text(
                text = book.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (focused) Modifier.basicMarquee() else Modifier),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = contentColor.copy(alpha = if (focused) 1f else 0.86f),
                maxLines = 1,
                overflow = if (focused) TextOverflow.Clip else TextOverflow.Ellipsis,
            )
            ReadingProgressBar(
                progress = if (book.hasReadingHistory()) book.progressRatio() else 0f,
                trackColor = contentColor.copy(alpha = if (focused) 0.16f else 0.14f),
                progressColor = contentColor.copy(alpha = if (book.hasReadingHistory()) 0.82f else 0.24f),
            )
        }
    }
}


@Composable
private fun BookCoverWithBadges(
    book: Book,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        BookCover(
            book = book,
            modifier = Modifier.fillMaxSize(),
        )
        BookKindChip(
            book = book,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp),
        )
    }
}


@Composable
private fun BookKindChip(
    book: Book,
    modifier: Modifier = Modifier,
) {
    BookStatusChip(
        text = book.kind().label,
        color = if (book.kind() == BookKind.Epub) Color(0xFF7DD3FC) else Color(0xFFFBBF24),
        modifier = modifier,
    )
}

@Composable
private fun BookMetaChips(
    book: Book,
    modifier: Modifier = Modifier,
) {
    BookKindChip(book = book, modifier = modifier)
}

@Composable
private fun BookStatusChip(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        colors = SurfaceDefaults.colors(containerColor = Color.Black.copy(alpha = 0.68f)),
        shape = MaterialTheme.shapes.small,
        border = Border(BorderStroke(1.dp, color.copy(alpha = 0.72f)), shape = MaterialTheme.shapes.small),
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun EmptyBookshelf(
    mode: BookshelfScreenMode,
) {
    val childPadding = rememberChildPadding()
    val title = if (mode == BookshelfScreenMode.Home) "书库暂无内容" else "书库还是空的"
    val message = if (mode == BookshelfScreenMode.Home) {
        "可以从顶部导航进入管理页, 用手机或电脑上传 TXT / EPUB。"
    } else {
        "导入 TXT / EPUB 后, 这里会显示最近导入的书籍。"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                start = childPadding.start,
                end = childPadding.end,
                top = 118.dp,
                bottom = 108.dp,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White.copy(alpha = 0.86f),
        )
        Text(
            text = message,
            modifier = Modifier.widthIn(max = 560.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.58f),
        )
    }
}

@Composable
private fun ReadingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    trackColor: Color = Color.White.copy(alpha = 0.14f),
    progressColor: Color = Color.White.copy(alpha = 0.82f),
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(trackColor, MaterialTheme.shapes.small),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                .height(4.dp)
                .background(progressColor, MaterialTheme.shapes.small),
        )
    }
}

@Composable
private fun DuskPageBackground(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF070D15)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFF17263A).copy(alpha = 0.52f), Color.Transparent),
                        radius = 980f,
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(Color(0xFF0B1420), Color(0xFF08111B), Color(0xFF070D15)),
                    ),
                ),
        )
        content()
    }
}


private fun continueSubtitle(book: Book, totalCount: Int): String {
    val author = book.author?.takeIf { it.isNotBlank() } ?: "未知作者"
    val progress = if (book.hasReadingHistory()) {
        "已读 ${(book.progressRatio() * 100).coerceIn(0f, 100f).toInt()}%"
    } else {
        "尚未开始"
    }
    return "$author · $progress · 书库 $totalCount 本"
}

private fun Modifier.focusRequesterIf(condition: Boolean, requester: FocusRequester): Modifier {
    return if (condition) focusRequester(requester) else this
}
