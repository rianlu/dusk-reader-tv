@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.wzl.duskreader.tv.presentation.screens.bookshelf

import android.view.KeyEvent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.FilterAlt
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
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
import com.wzl.duskreader.tv.presentation.common.DuskTvButton
import com.wzl.duskreader.tv.presentation.common.DuskTvButtonStyle
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding
import com.wzl.duskreader.tv.presentation.utils.requestFocusSafely
import com.wzl.duskreader.tv.tvmaterial.StandardDialog

private const val HOME_TOP_BAR_HIDE_THRESHOLD_PX = 300
private const val LIBRARY_TOP_BAR_HIDE_THRESHOLD_PX = 100
private const val LIBRARY_GRID_COLUMNS = 5
private val BOOK_POSTER_ASPECT_RATIO = 3f / 4f

/**
 * TV pivot 滚动:把聚焦行钉在视口约 35% 处。
 * 默认 BringIntoViewSpec 只在项贴边时才滚动,聚焦项落在视口边缘,
 * 下一行往往尚未组合,D-pad 焦点搜索找不到目标就跳到任意已组合项(表现为跳到第一/最后一个)。
 * pivot 让焦点行始终远离边缘,后续行提前组合,同时获得连续平滑的滚动手感。
 */
private val LibraryPivotBringIntoViewSpec = object : BringIntoViewSpec {
    override val scrollAnimationSpec: AnimationSpec<Float> = tween(
        durationMillis = 220,
        easing = LinearOutSlowInEasing,
    )

    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        return offset - containerSize * 0.35f
    }
}

enum class BookshelfScreenMode {
    Home,
    Library,
}

@Composable
fun BookshelfScreen(
    onBookClick: (book: Book) -> Unit,
    onGoTransfer: () -> Unit,
    onGoBookshelf: () -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    mode: BookshelfScreenMode = BookshelfScreenMode.Home,
    requestInitialFocusVersion: Long = 0L,
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
                            requestInitialFocusVersion = requestInitialFocusVersion,
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
                            requestInitialFocusVersion = requestInitialFocusVersion,
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
    requestInitialFocusVersion: Long,
) {
    val childPadding = rememberChildPadding()
    val listState = rememberLazyListState()
    val startRequester = remember { FocusRequester() }
    val featuredBook = remember(recentBooks, allBooks) { recentBooks.firstOrNull() ?: allBooks.first() }
    val hasRecentBook = remember(recentBooks) { recentBooks.isNotEmpty() }

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
    LaunchedEffect(requestInitialFocusVersion) {
        if (requestInitialFocusVersion > 0) startRequester.requestFocusSafely()
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
            ContinueReadingHero(
                book = featuredBook,
                totalCount = allBooks.size,
                hasRecentBook = hasRecentBook,
                startRequester = startRequester,
                onBookClick = { onBookClick(featuredBook) },
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
    requestInitialFocusVersion: Long,
) {
    val childPadding = rememberChildPadding()
    val gridState = rememberLazyGridState()
    val searchRequester = remember { FocusRequester() }
    val filterRequester = remember { FocusRequester() }
    val sortRequester = remember { FocusRequester() }
    var gridHasFocus by remember { mutableStateOf(false) }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var showSortDialog by remember { mutableStateOf(false) }

    val shouldShowTopBar by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 &&
                gridState.firstVisibleItemScrollOffset < LIBRARY_TOP_BAR_HIDE_THRESHOLD_PX
        }
    }
    LaunchedEffect(shouldShowTopBar, gridHasFocus) {
        onScroll(shouldShowTopBar && !gridHasFocus)
    }
    LaunchedEffect(requestInitialFocusVersion) {
        if (requestInitialFocusVersion > 0) searchRequester.requestFocusSafely()
    }
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
            filterRequester = filterRequester,
            sortRequester = sortRequester,
            hasResults = libraryBooks.isNotEmpty(),
            onSearchClick = { showSearchDialog = true },
            onFilterClick = { showFilterDialog = true },
            onSortClick = { showSortDialog = true },
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
        CompositionLocalProvider(LocalBringIntoViewSpec provides LibraryPivotBringIntoViewSpec) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(LIBRARY_GRID_COLUMNS),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { gridHasFocus = it.hasFocus }
                // 无回退目标的 focusRestorer:显式回退到某个网格项的 FocusRequester
                // 在该项被 Lazy 回收后会因"未附着"直接崩溃(快速滚动场景)
                .focusRestorer(),
            contentPadding = PaddingValues(
                start = childPadding.start,
                top = 10.dp,
                end = childPadding.end,
                bottom = 132.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            gridItemsIndexed(libraryBooks, key = { _, book -> book.id }) { index, book ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    LibraryBookTile(
                        book = book,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusProperties {
                                if (index % LIBRARY_GRID_COLUMNS == 0) {
                                    left = FocusRequester.Cancel
                                }
                                if (index % LIBRARY_GRID_COLUMNS == LIBRARY_GRID_COLUMNS - 1 ||
                                    index == libraryBooks.lastIndex
                                ) {
                                    right = FocusRequester.Cancel
                                }
                                if (index < LIBRARY_GRID_COLUMNS) {
                                    up = searchRequester
                                }
                                if (index + LIBRARY_GRID_COLUMNS > libraryBooks.lastIndex) {
                                    down = FocusRequester.Cancel
                                }
                            },
                        onClick = { onBookClick(book) },
                    )
                }
            }
        }
        }
    }

    LibrarySearchDialog(
        showDialog = showSearchDialog,
        query = liveSearchQuery,
        onQueryChange = onSearchQueryChange,
        onDismissRequest = { showSearchDialog = false },
    )
    LibraryOptionDialog(
        showDialog = showFilterDialog,
        title = "格式筛选",
        options = LibraryFormatFilter.entries,
        selected = formatFilter,
        label = { it.label },
        onSelect = {
            onSelectFormatFilter(it)
            showFilterDialog = false
        },
        onDismissRequest = { showFilterDialog = false },
    )
    LibraryOptionDialog(
        showDialog = showSortDialog,
        title = "排序方式",
        options = LibrarySort.entries,
        selected = librarySort,
        label = { it.label },
        onSelect = {
            onSelectLibrarySort(it)
            showSortDialog = false
        },
        onDismissRequest = { showSortDialog = false },
    )
}

@Composable
private fun ContinueReadingHero(
    book: Book,
    totalCount: Int,
    hasRecentBook: Boolean,
    startRequester: FocusRequester,
    onBookClick: () -> Unit,
) {
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
                    modifier = Modifier.focusRequester(startRequester),
                    onClick = onBookClick,
                )
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
    filterRequester: FocusRequester,
    sortRequester: FocusRequester,
    hasResults: Boolean,
    onSearchClick: () -> Unit,
    onFilterClick: () -> Unit,
    onSortClick: () -> Unit,
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DuskTvButton(
                text = searchLabel,
                icon = Icons.Outlined.Search,
                style = DuskTvButtonStyle.Secondary,
                onClick = onSearchClick,
                modifier = Modifier
                    .focusRequester(searchRequester)
                    .focusProperties {
                        right = filterRequester
                        // 有结果时不强制指向第 0 项(可能已被回收),交给默认 2D 焦点搜索
                        if (!hasResults) down = FocusRequester.Cancel
                    },
            )
            DuskTvButton(
                text = formatFilter.label,
                icon = Icons.Outlined.FilterAlt,
                style = DuskTvButtonStyle.Secondary,
                onClick = onFilterClick,
                modifier = Modifier
                    .focusRequester(filterRequester)
                    .focusProperties {
                        left = searchRequester
                        right = sortRequester
                        if (!hasResults) down = FocusRequester.Cancel
                    },
            )
            DuskTvButton(
                text = librarySort.label,
                icon = Icons.AutoMirrored.Outlined.Sort,
                style = DuskTvButtonStyle.Secondary,
                onClick = onSortClick,
                modifier = Modifier
                    .focusRequester(sortRequester)
                    .focusProperties {
                        left = filterRequester
                        right = FocusRequester.Cancel
                        if (!hasResults) down = FocusRequester.Cancel
                    },
            )
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

    LaunchedEffect(showDialog) {
        if (showDialog) inputRequester.requestFocusSafely()
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
                    .onFocusChanged { inputFocused = it.hasFocus }
                    .focusProperties {
                        left = FocusRequester.Cancel
                        right = FocusRequester.Cancel
                    }
                    .onPreviewKeyEvent { event ->
                        when (event.nativeKeyEvent.keyCode) {
                            KeyEvent.KEYCODE_DPAD_DOWN,
                            KeyEvent.KEYCODE_SYSTEM_NAVIGATION_DOWN -> {
                                if (event.type == KeyEventType.KeyDown) confirmRequester.requestFocusSafely()
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
private fun <T> LibraryOptionDialog(
    showDialog: Boolean,
    title: String,
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    onDismissRequest: () -> Unit,
) {
    val selectedRequester = remember { FocusRequester() }

    LaunchedEffect(showDialog) {
        if (showDialog) selectedRequester.requestFocusSafely()
    }

    StandardDialog(
        showDialog = showDialog,
        onDismissRequest = onDismissRequest,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { option ->
                    val isSelected = option == selected
                    var focused by remember { mutableStateOf(false) }
                    Surface(
                        onClick = { onSelect(option) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequesterIf(isSelected, selectedRequester)
                            .onFocusChanged { focused = it.hasFocus }
                            .focusProperties {
                                left = FocusRequester.Cancel
                                right = FocusRequester.Cancel
                            },
                        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
                        colors = ClickableSurfaceDefaults.colors(
                            containerColor = if (isSelected) {
                                Color.White.copy(alpha = 0.16f)
                            } else {
                                Color.White.copy(alpha = 0.08f)
                            },
                            contentColor = Color.White,
                            focusedContainerColor = Color.White,
                            focusedContentColor = Color.Black,
                        ),
                        border = ClickableSurfaceDefaults.border(
                            focusedBorder = Border(
                                border = BorderStroke(2.dp, Color.White),
                                shape = MaterialTheme.shapes.medium,
                            ),
                        ),
                        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = label(option),
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                ),
                            )
                            if (isSelected) {
                                Text(
                                    text = "✓",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = if (focused) Color.Black else Color.White,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
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
            focusedBorder = Border(
                border = BorderStroke(3.dp, Color.White),
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
private fun SectionHeader(
    title: String,
    subtitle: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.58f),
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
