package com.wzl.duskreader.tv.presentation.screens.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.BookList
import com.wzl.duskreader.tv.presentation.common.BookCard
import com.wzl.duskreader.tv.presentation.common.BookCover
import com.wzl.duskreader.tv.presentation.common.Loading
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding

@Composable
fun LibraryScreen(
    onBookClick: (book: Book) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    viewModel: LibraryScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is LibraryScreenUiState.Loading -> Loading(modifier = Modifier.fillMaxSize())
        is LibraryScreenUiState.Ready -> {
            if (s.books.isEmpty()) {
                EmptyLibrary()
            } else {
                BookGrid(
                    books = s.books,
                    onBookClick = onBookClick,
                    onScroll = onScroll,
                    isTopBarVisible = isTopBarVisible,
                )
            }
        }
    }
}

@Composable
private fun BookGrid(
    books: BookList,
    onBookClick: (book: Book) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
) {
    val gridState = rememberLazyGridState()
    val childPadding = rememberChildPadding()

    val shouldShowTopBar by remember {
        derivedStateOf {
            gridState.firstVisibleItemIndex == 0 &&
                gridState.firstVisibleItemScrollOffset < 300
        }
    }

    LaunchedEffect(shouldShowTopBar) { onScroll(shouldShowTopBar) }
    LaunchedEffect(isTopBarVisible) {
        if (isTopBarVisible) gridState.animateScrollToItem(0)
    }

    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(6),
        contentPadding = PaddingValues(
            start = childPadding.start,
            end = childPadding.end,
            top = 16.dp,
            bottom = 108.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(books, key = { it.id }) { book ->
            BookCard(
                onClick = { onBookClick(book) },
                image = {
                    BookCover(
                        book = book,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(10.5f / 16f),
                    )
                },
                title = {
                    Text(
                        text = book.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                },
            )
        }
    }
}

@Composable
private fun EmptyLibrary() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "书架为空，通过「传书」上传 TXT 文件",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
