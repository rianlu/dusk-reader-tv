package com.wzl.duskreader.tv.presentation.screens.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.BookList
import com.wzl.duskreader.tv.presentation.common.BooksRow
import com.wzl.duskreader.tv.presentation.common.Error
import com.wzl.duskreader.tv.presentation.common.Loading

@Composable
fun HomeScreen(
    onBookClick: (book: Book) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    homeScreenViewModel: HomeScreenViewModel = hiltViewModel(),
) {
    val uiState by homeScreenViewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is HomeScreenUiState.Ready -> {
            Catalog(
                recentBooks = s.recentBooks,
                allBooks = s.allBooks,
                onBookClick = onBookClick,
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
    recentBooks: BookList,
    allBooks: BookList,
    onBookClick: (book: Book) -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    modifier: Modifier = Modifier,
    isTopBarVisible: Boolean = true,
) {
    val lazyListState = rememberLazyListState()

    val shouldShowTopBar by remember {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex == 0 &&
                lazyListState.firstVisibleItemScrollOffset < 300
        }
    }

    LaunchedEffect(shouldShowTopBar) { onScroll(shouldShowTopBar) }
    LaunchedEffect(isTopBarVisible) {
        if (isTopBarVisible) lazyListState.animateScrollToItem(0)
    }

    if (allBooks.isEmpty()) {
        EmptyHome(modifier = modifier)
        return
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(bottom = 108.dp),
        modifier = modifier,
    ) {
        if (recentBooks.isNotEmpty()) {
            item(contentType = "RecentBooksRow") {
                BooksRow(
                    modifier = Modifier.padding(top = 16.dp),
                    bookList = recentBooks,
                    title = "最近阅读",
                    onBookSelected = onBookClick,
                )
            }
        }
        item(contentType = "AllBooksRow") {
            BooksRow(
                modifier = Modifier.padding(top = 16.dp),
                bookList = allBooks,
                title = "全部书库",
                onBookSelected = onBookClick,
            )
        }
    }
}

@Composable
private fun EmptyHome(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = "书架为空，通过「传书」上传 TXT 文件开始阅读",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
