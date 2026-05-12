@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.wzl.duskreader.tv.presentation.screens.bookshelf

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.BookList
import com.wzl.duskreader.tv.data.entities.hasReadingHistory
import com.wzl.duskreader.tv.data.entities.progressRatio
import com.wzl.duskreader.tv.presentation.common.BookCard
import com.wzl.duskreader.tv.presentation.common.BookCover
import com.wzl.duskreader.tv.presentation.common.Loading
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding
import java.util.Locale

private const val RECENT_READING_LIMIT = 8
private const val LIBRARY_ROW_LIMIT = 120
private const val LIBRARY_GRID_COLUMNS = 5
private val POSTER_ASPECT_RATIO = 10.5f / 16f
private val RAIL_CARD_WIDTH = 154.dp

enum class BookshelfScreenMode {
    Home,
    Library,
}

private enum class LibrarySortMode(val label: String) {
    ImportedDesc("最近导入"),
    TitleAsc("书名排序"),
    ReadingProgress("阅读进度"),
}

@Composable
fun BookshelfScreen(
    onBookClick: (book: Book) -> Unit,
    onGoTransfer: () -> Unit,
    onGoBookshelf: () -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    mode: BookshelfScreenMode = BookshelfScreenMode.Home,
    requestInitialFocus: Boolean = false,
    viewModel: BookshelfScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rescanState by viewModel.rescanState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is BookshelfUiState.Loading -> Loading(modifier = Modifier.fillMaxSize())
        is BookshelfUiState.Ready -> {
            if (state.allBooks.isEmpty()) {
                EmptyBookshelf(
                    onGoTransfer = onGoTransfer,
                    onRefresh = viewModel::rescanLibrary,
                    rescanState = rescanState,
                )
            } else {
                when (mode) {
                    BookshelfScreenMode.Home -> ReadingHome(
                        recentBooks = state.recentBooks,
                        allBooks = state.allBooks,
                        onBookClick = onBookClick,
                        onGoBookshelf = onGoBookshelf,
                        onScroll = onScroll,
                        requestInitialFocus = requestInitialFocus,
                    )

                    BookshelfScreenMode.Library -> LibraryPosterWall(
                        allBooks = state.allBooks,
                        rescanState = rescanState,
                        onBookClick = onBookClick,
                        onRefresh = viewModel::rescanLibrary,
                        onScroll = onScroll,
                        isTopBarVisible = isTopBarVisible,
                        requestInitialFocus = requestInitialFocus,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReadingHome(
    recentBooks: BookList,
    allBooks: BookList,
    onBookClick: (book: Book) -> Unit,
    onGoBookshelf: () -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    requestInitialFocus: Boolean,
) {
    val childPadding = rememberChildPadding()
    val continueBook = remember(recentBooks, allBooks) { recentBooks.firstOrNull() ?: allBooks.first() }
    val recentReadingBooks = remember(recentBooks, continueBook) {
        recentBooks.filterNot { it.id == continueBook.id }.take(RECENT_READING_LIMIT)
    }
    val continueRequester = remember { FocusRequester() }
    val libraryRequester = remember { FocusRequester() }
    val recentFirstRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { onScroll(true) }
    LaunchedEffect(continueBook.id, requestInitialFocus) {
        if (requestInitialFocus) continueRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070B)),
    ) {
        HomeBackground()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = childPadding.start,
                end = childPadding.end,
                top = 34.dp,
                bottom = 96.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item {
                ContinueReadingStage(
                    book = continueBook,
                    totalCount = allBooks.size,
                    continueRequester = continueRequester,
                    libraryRequester = libraryRequester,
                    downRequester = if (recentReadingBooks.isNotEmpty()) recentFirstRequester else FocusRequester.Cancel,
                    onContinue = { onBookClick(continueBook) },
                    onGoBookshelf = onGoBookshelf,
                )
            }
            if (recentReadingBooks.isNotEmpty()) {
                item {
                    RecentReadingRail(
                        books = recentReadingBooks,
                        firstItemRequester = recentFirstRequester,
                        upRequester = continueRequester,
                        onBookClick = onBookClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryPosterWall(
    allBooks: BookList,
    rescanState: RescanState,
    onBookClick: (book: Book) -> Unit,
    onRefresh: () -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    requestInitialFocus: Boolean,
) {
    val childPadding = rememberChildPadding()
    val refreshRequester = remember { FocusRequester() }
    val sortFirstRequester = remember { FocusRequester() }
    val firstBookRequester = remember { FocusRequester() }
    var sortMode by remember { mutableStateOf(LibrarySortMode.ImportedDesc) }
    val sortedBooks = remember(allBooks, sortMode) {
        when (sortMode) {
            LibrarySortMode.ImportedDesc -> allBooks.sortedByDescending { it.importedAt }
            LibrarySortMode.TitleAsc -> allBooks.sortedBy { it.title.lowercase(Locale.ROOT) }
            LibrarySortMode.ReadingProgress -> allBooks.sortedWith(
                compareByDescending<Book> { it.hasReadingHistory() }
                    .thenByDescending { it.progressRatio() }
                    .thenBy { it.title.lowercase(Locale.ROOT) },
            )
        }.take(LIBRARY_ROW_LIMIT)
    }

    LaunchedEffect(Unit) { onScroll(true) }
    LaunchedEffect(isTopBarVisible) { if (!isTopBarVisible) onScroll(true) }
    LaunchedEffect(allBooks.firstOrNull()?.id, requestInitialFocus) {
        if (requestInitialFocus) firstBookRequester.requestFocus()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF05070B)),
    ) {
        HomeBackground()
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .focusRestorer(),
            contentPadding = PaddingValues(
                top = 34.dp,
                bottom = 108.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item {
                LibraryHeader(
                    totalCount = allBooks.size,
                    shownCount = sortedBooks.size,
                    sortMode = sortMode,
                    rescanState = rescanState,
                    refreshRequester = refreshRequester,
                    sortFirstRequester = sortFirstRequester,
                    downRequester = firstBookRequester,
                    onSortChange = { sortMode = it },
                    onRefresh = onRefresh,
                    modifier = Modifier.padding(start = childPadding.start, end = childPadding.end),
                )
            }

            item {
                LibraryBookGrid(
                    books = sortedBooks,
                    firstItemRequester = firstBookRequester,
                    upRequester = sortFirstRequester,
                    onBookClick = onBookClick,
                )
            }
        }
    }
}

@Composable
private fun HomeBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF17243A),
                        Color(0xFF0A111B),
                        Color(0xFF05070B),
                    ),
                ),
            ),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.18f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.36f),
                    ),
                ),
            ),
    )
}

@Composable
private fun ContinueReadingStage(
    book: Book,
    totalCount: Int,
    continueRequester: FocusRequester,
    libraryRequester: FocusRequester,
    downRequester: FocusRequester,
    onContinue: () -> Unit,
    onGoBookshelf: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(360.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = SurfaceDefaults.colors(containerColor = Color(0xFF101722)),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF080B12),
                                Color(0xFF172744),
                                Color(0xFF0A0D13),
                            ),
                        ),
                    ),
            )
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 42.dp, vertical = 34.dp),
                horizontalArrangement = Arrangement.spacedBy(36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BookCover(
                    book = book,
                    modifier = Modifier
                        .width(176.dp)
                        .aspectRatio(POSTER_ASPECT_RATIO),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = "继续阅读",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White.copy(alpha = 0.66f),
                    )
                    Text(
                        text = book.title,
                        style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = continueSubtitle(book, totalCount),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.72f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    ReadingProgressBar(progress = book.progressRatio())
                    Row(
                        modifier = Modifier.focusGroup(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Button(
                            onClick = onContinue,
                            modifier = Modifier
                                .focusRequester(continueRequester)
                                .focusProperties {
                                    left = FocusRequester.Cancel
                                    down = downRequester
                                },
                            shape = ButtonDefaults.shape(MaterialTheme.shapes.large),
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (book.hasReadingHistory()) "继续阅读" else "开始阅读")
                        }
                        Button(
                            onClick = onGoBookshelf,
                            modifier = Modifier
                                .focusRequester(libraryRequester)
                                .focusProperties {
                                    right = FocusRequester.Cancel
                                    down = downRequester
                                },
                            shape = ButtonDefaults.shape(MaterialTheme.shapes.large),
                            colors = ButtonDefaults.colors(
                                containerColor = Color.White.copy(alpha = 0.10f),
                                contentColor = Color.White,
                            ),
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.LibraryBooks,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("进入书库")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryHeader(
    totalCount: Int,
    shownCount: Int,
    sortMode: LibrarySortMode,
    rescanState: RescanState,
    refreshRequester: FocusRequester,
    sortFirstRequester: FocusRequester,
    downRequester: FocusRequester,
    onSortChange: (LibrarySortMode) -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom,
        ) {
            SectionHeader(
                title = "全部书库",
                subtitle = "共 $totalCount 本 · 当前显示 $shownCount 本 · ${rescanState.label()}",
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onRefresh,
                modifier = Modifier
                    .focusRequester(refreshRequester)
                    .focusProperties {
                        right = FocusRequester.Cancel
                        down = downRequester
                    },
                shape = ButtonDefaults.shape(MaterialTheme.shapes.large),
                colors = ButtonDefaults.colors(
                    containerColor = Color.White.copy(alpha = 0.10f),
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (rescanState is RescanState.Scanning) "扫描中…" else "刷新书库")
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "排序",
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.62f),
            )
            LibrarySortMode.entries.forEachIndexed { index, mode ->
                SortChip(
                    label = mode.label,
                    selected = sortMode == mode,
                    modifier = Modifier
                        .then(if (index == 0) Modifier.focusRequester(sortFirstRequester) else Modifier)
                        .focusProperties {
                            if (index == 0) left = FocusRequester.Cancel
                            if (index == LibrarySortMode.entries.lastIndex) right = refreshRequester
                            down = downRequester
                        },
                    onClick = { onSortChange(mode) },
                )
            }
        }
    }
}

@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) Color.White.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.07f),
            focusedContainerColor = Color.White,
            contentColor = Color.White,
            focusedContentColor = Color.Black,
        ),
        border = ClickableSurfaceDefaults.border(
            border = if (selected) Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.42f))) else Border.None,
            focusedBorder = Border(border = BorderStroke(2.dp, Color.White), shape = MaterialTheme.shapes.large),
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun LibraryBookGrid(
    books: BookList,
    firstItemRequester: FocusRequester,
    upRequester: FocusRequester,
    onBookClick: (book: Book) -> Unit,
) {
    val childPadding = rememberChildPadding()
    val gridState = rememberLazyGridState()

    LazyVerticalGrid(
        columns = GridCells.Fixed(LIBRARY_GRID_COLUMNS),
        state = gridState,
        modifier = Modifier
            .fillMaxWidth()
            .height(620.dp)
            .focusRestorer(),
        contentPadding = PaddingValues(
            start = childPadding.start,
            end = childPadding.end,
            top = 8.dp,
            bottom = 52.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        gridItemsIndexed(books, key = { _, book -> book.id }) { index, book ->
            LibraryBookCard(
                book = book,
                modifier = Modifier
                    .focusRequesterIf(index == 0, firstItemRequester)
                    .focusProperties {
                        if (index < LIBRARY_GRID_COLUMNS) up = upRequester
                        if (index % LIBRARY_GRID_COLUMNS == 0) left = FocusRequester.Cancel
                        if (index % LIBRARY_GRID_COLUMNS == LIBRARY_GRID_COLUMNS - 1 || index == books.lastIndex) {
                            right = FocusRequester.Cancel
                        }
                        if (index >= books.size - LIBRARY_GRID_COLUMNS) down = FocusRequester.Cancel
                    },
                onClick = { onBookClick(book) },
            )
        }
    }
}

@Composable
private fun LibraryBookCard(
    book: Book,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    BookCard(
        onClick = onClick,
        modifier = modifier,
        image = {
            BookCover(
                book = book,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(POSTER_ASPECT_RATIO),
            )
        },
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (book.hasReadingHistory()) {
                    ReadingProgressBar(progress = book.progressRatio())
                }
            }
        },
    )
}

@Composable
private fun RecentReadingRail(
    books: BookList,
    firstItemRequester: FocusRequester,
    upRequester: FocusRequester,
    onBookClick: (book: Book) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionHeader(
            title = "最近阅读",
            subtitle = "只保留最近打开的书, 方便快速切换",
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(18.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 8.dp, end = 24.dp),
            modifier = Modifier.focusRestorer(),
        ) {
            itemsIndexed(books, key = { _, book -> book.id }) { index, book ->
                PosterTile(
                    book = book,
                    modifier = Modifier
                        .width(RAIL_CARD_WIDTH)
                        .focusRequesterIf(index == 0, firstItemRequester)
                        .focusProperties {
                            up = upRequester
                            down = FocusRequester.Cancel
                            if (index == 0) left = FocusRequester.Cancel
                            if (index == books.lastIndex) right = FocusRequester.Cancel
                        },
                    onClick = { onBookClick(book) },
                )
            }
        }
    }
}

@Composable
private fun PosterTile(
    book: Book,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent,
        ),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(border = BorderStroke(2.dp, Color.White), shape = MaterialTheme.shapes.medium),
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(elevation = 14.dp, elevationColor = Color.White.copy(alpha = 0.18f)),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.06f),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            BookCover(
                book = book,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(POSTER_ASPECT_RATIO),
            )
            Text(
                text = book.title,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (book.hasReadingHistory()) {
                ReadingProgressBar(progress = book.progressRatio())
            }
        }
    }
}

@Composable
private fun ReadingProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .background(Color.White.copy(alpha = 0.14f), MaterialTheme.shapes.small),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.86f), MaterialTheme.shapes.small),
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        )
    }
}

private fun RescanState.label(): String = when (this) {
    RescanState.Idle -> "可重新扫描 Documents/暮阅"
    RescanState.Scanning -> "正在扫描…"
    is RescanState.Done -> if (imported > 0) "刚刚新增 $imported 本" else "刚刚扫描完成, 无新增"
    is RescanState.Failure -> "扫描失败: $message"
}

private fun continueSubtitle(book: Book, totalCount: Int): String {
    val progress = if (book.hasReadingHistory()) {
        "已读 ${String.format(Locale.ROOT, "%.0f%%", book.progressRatio() * 100)}"
    } else {
        "未开始"
    }
    val author = book.author?.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()
    return "$progress$author · 书库共 $totalCount 本"
}

@Composable
private fun EmptyBookshelf(
    onGoTransfer: () -> Unit,
    onRefresh: () -> Unit,
    rescanState: RescanState,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.07f)),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 44.dp, vertical = 36.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "书库还没有书",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                )
                Text(
                    text = "通过传书上传 TXT / EPUB, 或把文件放进 Documents/暮阅 后刷新。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(onClick = onGoTransfer) { Text("前往传书") }
                    Button(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (rescanState is RescanState.Scanning) "扫描中…" else "刷新书库")
                    }
                }
            }
        }
    }
}

private fun Modifier.focusRequesterIf(condition: Boolean, requester: FocusRequester): Modifier {
    return if (condition) focusRequester(requester) else this
}
