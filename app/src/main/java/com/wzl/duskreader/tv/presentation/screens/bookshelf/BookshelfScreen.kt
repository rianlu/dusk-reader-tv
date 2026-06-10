@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.wzl.duskreader.tv.presentation.screens.bookshelf

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
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
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import com.wzl.duskreader.tv.data.entities.hasGeneratedCover
import com.wzl.duskreader.tv.data.entities.hasOpenDataCover
import com.wzl.duskreader.tv.data.entities.kind
import com.wzl.duskreader.tv.data.entities.hasReadingHistory
import com.wzl.duskreader.tv.data.entities.progressRatio
import com.wzl.duskreader.tv.presentation.common.BookCover
import com.wzl.duskreader.tv.presentation.common.DuskTvButton
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding

private const val HOME_TOP_BAR_HIDE_THRESHOLD_PX = 300
private const val LIBRARY_TOP_BAR_HIDE_THRESHOLD_PX = 100
private const val LIBRARY_GRID_COLUMNS = 5
private const val LIBRARY_LIMIT = 240
private val BOOK_POSTER_ASPECT_RATIO = 3f / 4f

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
    requestInitialFocus: Boolean = false,
    viewModel: BookshelfScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    DuskPageBackground {
        when (val state = uiState) {
            is BookshelfUiState.Loading -> Unit
            is BookshelfUiState.Ready -> {
                if (state.allBooks.isEmpty()) {
                    EmptyBookshelf(onGoTransfer = onGoTransfer)
                } else {
                    when (mode) {
                        BookshelfScreenMode.Home -> HomeBookshelf(
                            recentBooks = state.recentBooks,
                            allBooks = state.allBooks,
                            onBookClick = onBookClick,
                            onScroll = onScroll,
                            isTopBarVisible = isTopBarVisible,
                            requestInitialFocus = requestInitialFocus,
                        )

                        BookshelfScreenMode.Library -> LibraryBookshelf(
                            allBooks = state.allBooks,
                            onBookClick = onBookClick,
                            onScroll = onScroll,
                            isTopBarVisible = isTopBarVisible,
                            requestInitialFocus = requestInitialFocus,
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
    requestInitialFocus: Boolean,
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
    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) startRequester.requestFocus()
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
    onBookClick: (book: Book) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    requestInitialFocus: Boolean,
) {
    val childPadding = rememberChildPadding()
    val gridState = rememberLazyGridState()
    val firstBookRequester = remember { FocusRequester() }
    var gridHasFocus by remember { mutableStateOf(false) }
    val sortedBooks = remember(allBooks) {
        allBooks.sortedByDescending { it.importedAt }.take(LIBRARY_LIMIT)
    }

    val shouldShowTopBar by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 &&
                gridState.firstVisibleItemScrollOffset < LIBRARY_TOP_BAR_HIDE_THRESHOLD_PX
        }
    }
    LaunchedEffect(shouldShowTopBar, gridHasFocus) {
        onScroll(shouldShowTopBar && !gridHasFocus)
    }
    LaunchedEffect(requestInitialFocus) {
        if (requestInitialFocus) firstBookRequester.requestFocus()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LibraryHeader(
            books = allBooks,
            shownCount = sortedBooks.size,
            modifier = Modifier.padding(
                start = childPadding.start,
                end = childPadding.end,
                top = 8.dp,
                bottom = 8.dp,
            ),
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(LIBRARY_GRID_COLUMNS),
            state = gridState,
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { gridHasFocus = it.hasFocus }
                .focusRestorer { firstBookRequester },
            contentPadding = PaddingValues(
                start = childPadding.start,
                top = 10.dp,
                end = childPadding.end,
                bottom = 132.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            gridItemsIndexed(sortedBooks, key = { _, book -> book.id }) { index, book ->
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
                            .focusRequesterIf(index == 0, firstBookRequester)
                            .focusProperties {
                                if (index % LIBRARY_GRID_COLUMNS == 0) {
                                    left = FocusRequester.Cancel
                                }
                            },
                        onClick = { onBookClick(book) },
                    )
                }
            }
        }
    }
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
private fun LibraryHeader(
    books: BookList,
    shownCount: Int,
    modifier: Modifier = Modifier,
) {
    val totalCount = books.size
    val txtCount = books.count { it.kind() == BookKind.Novel }
    val epubCount = books.count { it.kind() == BookKind.Epub }
    val openCoverCount = books.count { it.hasOpenDataCover() }
    val generatedCoverCount = books.count { it.hasGeneratedCover() }
    val shownText = if (totalCount == shownCount) "最近导入" else "显示 $shownCount 本 · 最近导入"
    val summary = "共 $totalCount 本 · TXT $txtCount · EPUB $epubCount · 开放源 $openCoverCount · 生成 $generatedCoverCount · $shownText"
    Text(
        text = summary,
        modifier = modifier.fillMaxWidth(),
        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        color = Color.White.copy(alpha = 0.58f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
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
    onGoTransfer: () -> Unit,
) {
    val childPadding = rememberChildPadding()
    val transferRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { transferRequester.requestFocus() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = childPadding.start, vertical = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.08f)),
            shape = MaterialTheme.shapes.extraLarge,
            border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.14f)), shape = MaterialTheme.shapes.extraLarge),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 44.dp, vertical = 38.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Surface(
                    modifier = Modifier.size(58.dp),
                    colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.12f)),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(text = "书", style = MaterialTheme.typography.titleLarge, color = Color.White)
                    }
                }
                Text(
                    text = "书库还没有书",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color.White,
                )
                Text(
                    text = "通过局域网书库管理导入 TXT / EPUB, 本地目录扫描可在设置页执行.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.68f),
                )
                Row(
                    modifier = Modifier.focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DuskTvButton(
                        text = "去管理",
                        modifier = Modifier.focusRequester(transferRequester),
                        onClick = onGoTransfer,
                    )
                }
            }
        }
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
