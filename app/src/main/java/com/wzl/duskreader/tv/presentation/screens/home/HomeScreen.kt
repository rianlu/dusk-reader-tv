package com.wzl.duskreader.tv.presentation.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.progressRatio
import com.wzl.duskreader.tv.presentation.common.BookBackdrop
import com.wzl.duskreader.tv.presentation.common.BooksRow
import com.wzl.duskreader.tv.presentation.common.Error
import com.wzl.duskreader.tv.presentation.common.Loading
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding
import java.util.Locale

@Composable
fun HomeScreen(
    onBookClick: (book: Book) -> Unit,
    onGoTransfer: () -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel(),
) {
    val uiState by homeScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is HomeScreenUiState.Ready -> {
            Catalog(
                stage = s.stage,
                onBookClick = onBookClick,
                onGoTransfer = onGoTransfer,
                onScroll = onScroll,
                isTopBarVisible = isTopBarVisible,
                modifier = Modifier.fillMaxSize(),
            )
        }

        is HomeScreenUiState.Loading -> Loading(modifier = Modifier.fillMaxSize())
        is HomeScreenUiState.Error -> Error(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun Catalog(
    stage: HomeStage,
    onBookClick: (book: Book) -> Unit,
    onGoTransfer: () -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isTopBarVisible: Boolean = true,
) {
    val lazyListState = rememberLazyListState()

    val shouldShowTopBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 &&
                lazyListState.firstVisibleItemScrollOffset < 280
        }
    }

    LaunchedEffect(shouldShowTopBar) { onScroll(shouldShowTopBar) }
    LaunchedEffect(isTopBarVisible) {
        if (isTopBarVisible) lazyListState.animateScrollToItem(0)
    }

    if (stage.shelves.isEmpty()) {
        EmptyHome(
            modifier = modifier,
            onGoTransfer = onGoTransfer,
        )
        return
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(bottom = 108.dp),
        modifier = modifier,
    ) {
        stage.featuredBook?.let { book ->
            item(contentType = "HomeStageHero") {
                HomeStageHero(
                    book = book,
                    onOpenBook = { onBookClick(book) },
                )
            }
        }

        items(stage.shelves.size, key = { index -> stage.shelves[index].title }) { index ->
            val shelf = stage.shelves[index]
            BooksRow(
                modifier = Modifier.padding(top = if (index == 0) 28.dp else 20.dp),
                bookList = shelf.books,
                title = shelf.title,
                onBookSelected = onBookClick,
            )
        }
    }
}

@Composable
private fun HomeStageHero(
    book: Book,
    onOpenBook: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val childPadding = rememberChildPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(352.dp),
    ) {
        BookBackdrop(
            book = book,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.82f),
                            Color.Black.copy(alpha = 0.55f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    start = childPadding.start,
                    end = childPadding.end,
                    bottom = 28.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stageEyebrowForBook(book),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.68f),
            )
            Text(
                text = book.title,
                style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
            )
            if (!book.author.isNullOrBlank()) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White.copy(alpha = 0.78f),
                )
            }
            Text(
                text = stageSubtitleForBook(book),
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.74f),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(onClick = onOpenBook) {
                    Text(if (book.lastReadPosition > 0) "继续阅读" else "开始阅读")
                }
            }
        }
    }
}

private fun stageEyebrowForBook(book: Book): String {
    return if (book.lastReadPosition > 0) "继续阅读" else "书库精选"
}

private fun stageSubtitleForBook(book: Book): String {
    return if (book.lastReadPosition > 0) {
        "当前进度 ${String.format(Locale.ROOT, "%.0f%%", book.progressRatio() * 100)}"
    } else {
        "已导入到本地书库，可从详情页进入阅读"
    }
}

@Composable
private fun EmptyHome(
    onGoTransfer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "书架为空，通过「传书」导入小说开始浏览",
                style = MaterialTheme.typography.titleMedium,
            )
            Spacer(Modifier.height(16.dp))
            Button(onClick = onGoTransfer) {
                Text("前往传书")
            }
        }
    }
}
