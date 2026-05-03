@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.wzl.duskreader.tv.presentation.screens.bookshelf

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.BookList
import com.wzl.duskreader.tv.presentation.common.BookCard
import com.wzl.duskreader.tv.presentation.common.BookCover
import com.wzl.duskreader.tv.presentation.common.Loading
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding

private const val GRID_COLUMNS = 4
private val STRIP_CARD_WIDTH = 220.dp
private val POSTER_ASPECT_RATIO = 10.5f / 16f

@Composable
fun BookshelfScreen(
    onBookClick: (book: Book) -> Unit,
    onGoTransfer: () -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    viewModel: BookshelfScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val rescanState by viewModel.rescanState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is BookshelfUiState.Loading -> Loading(modifier = Modifier.fillMaxSize())
        is BookshelfUiState.Ready -> {
            if (s.allBooks.isEmpty()) {
                EmptyBookshelf(
                    onGoTransfer = onGoTransfer,
                    onRefresh = viewModel::rescanLibrary,
                    rescanState = rescanState,
                )
            } else {
                BookshelfGrid(
                    recentBooks = s.recentBooks,
                    allBooks = s.allBooks,
                    rescanState = rescanState,
                    onBookClick = onBookClick,
                    onRefresh = viewModel::rescanLibrary,
                    onScroll = onScroll,
                    isTopBarVisible = isTopBarVisible,
                )
            }
        }
    }
}

@Composable
private fun BookshelfGrid(
    recentBooks: BookList,
    allBooks: BookList,
    rescanState: RescanState,
    onBookClick: (book: Book) -> Unit,
    onRefresh: () -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
) {
    val gridState = rememberLazyGridState()
    val childPadding = rememberChildPadding()

    val shouldShowTopBar by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 &&
                gridState.firstVisibleItemScrollOffset < 260
        }
    }

    LaunchedEffect(shouldShowTopBar) { onScroll(shouldShowTopBar) }
    LaunchedEffect(isTopBarVisible) {
        if (isTopBarVisible) gridState.animateScrollToItem(0)
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(GRID_COLUMNS),
        contentPadding = PaddingValues(
            start = childPadding.start,
            end = childPadding.end,
            top = 28.dp,
            bottom = 108.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(32.dp),
        verticalArrangement = Arrangement.spacedBy(36.dp),
        modifier = Modifier
            .fillMaxSize()
            .focusRestorer(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            BookshelfHeader(
                totalCount = allBooks.size,
                rescanState = rescanState,
                onRefresh = onRefresh,
            )
        }

        if (recentBooks.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ContinueReadingStrip(
                    books = recentBooks,
                    onBookClick = onBookClick,
                )
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                text = "全部书库",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
            )
        }

        items(allBooks, key = { it.id }) { book ->
            BookCard(
                onClick = { onBookClick(book) },
                image = {
                    BookCover(
                        book = book,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(POSTER_ASPECT_RATIO),
                    )
                },
            )
        }
    }
}

@Composable
private fun BookshelfHeader(
    totalCount: Int,
    rescanState: RescanState,
    onRefresh: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "书架",
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
            )
            Text(
                text = "共 $totalCount 本 · ${rescanState.label()}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            )
        }

        Button(
            onClick = onRefresh,
            modifier = Modifier.height(48.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(text = if (rescanState is RescanState.Scanning) "扫描中…" else "刷新书库")
        }
    }
}

private fun RescanState.label(): String = when (this) {
    RescanState.Idle -> "顶部「刷新书库」可重新扫描 Documents/暮阅"
    RescanState.Scanning -> "正在扫描…"
    is RescanState.Done -> if (imported > 0) "刚刚新增 $imported 本" else "刚刚扫描完成（无新增）"
    is RescanState.Failure -> "扫描失败：$message"
}

@Composable
private fun ContinueReadingStrip(
    books: BookList,
    onBookClick: (book: Book) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = "在读",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        )
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(
                top = 12.dp,
                bottom = 16.dp,
                end = 24.dp,
            ),
            modifier = Modifier.focusRestorer(),
        ) {
            items(books, key = { it.id }) { book ->
                ContinueReadingCard(
                    book = book,
                    onClick = { onBookClick(book) },
                )
            }
        }
    }
}

@Composable
private fun ContinueReadingCard(
    book: Book,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.width(STRIP_CARD_WIDTH),
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.medium),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(
                border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                shape = MaterialTheme.shapes.medium,
            ),
        ),
        glow = ClickableSurfaceDefaults.glow(
            focusedGlow = Glow(
                elevation = 12.dp,
                elevationColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f),
            ),
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.04f),
    ) {
        BookCover(
            book = book,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(POSTER_ASPECT_RATIO),
        )
    }
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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "书架还没有书",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "通过「传书」从手机上传 TXT / EPUB，或把文件放进 Documents/暮阅 后点刷新",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
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
