package com.wzl.duskreader.tv.presentation.screens.library

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
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
    onGoTransfer: () -> Unit,
    onScroll: (isTopBarVisible: Boolean) -> Unit,
    isTopBarVisible: Boolean,
    viewModel: LibraryScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val s = uiState) {
        is LibraryScreenUiState.Loading -> Loading(modifier = Modifier.fillMaxSize())
        is LibraryScreenUiState.Ready -> {
            if (!s.hasAnyBooks) {
                EmptyLibrary(onGoTransfer = onGoTransfer)
            } else {
                PosterLibraryGrid(
                    books = s.books,
                    selectedSort = s.selectedSort,
                    selectedFilter = s.selectedFilter,
                    onBookClick = onBookClick,
                    onSortChange = viewModel::updateSort,
                    onFilterChange = viewModel::updateFilter,
                    onScroll = onScroll,
                    isTopBarVisible = isTopBarVisible,
                )
            }
        }
    }
}

@Composable
private fun PosterLibraryGrid(
    books: BookList,
    selectedSort: LibrarySortOption,
    selectedFilter: LibraryFormatFilter,
    onBookClick: (book: Book) -> Unit,
    onSortChange: (LibrarySortOption) -> Unit,
    onFilterChange: (LibraryFormatFilter) -> Unit,
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
        columns = GridCells.Fixed(6),
        contentPadding = PaddingValues(
            start = childPadding.start,
            end = childPadding.end,
            top = 28.dp,
            bottom = 108.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            LibraryControls(
                selectedSort = selectedSort,
                selectedFilter = selectedFilter,
                onSortChange = onSortChange,
                onFilterChange = onFilterChange,
            )
        }

        items(books, key = { it.id }) { book ->
            BookCard(
                modifier = Modifier.width(184.dp),
                onClick = { onBookClick(book) },
                image = {
                    BookCover(
                        book = book,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(10.5f / 16f),
                    )
                },
            )
        }

        if (books.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                EmptyFilterResult()
            }
        }
    }
}

@Composable
private fun LibraryControls(
    selectedSort: LibrarySortOption,
    selectedFilter: LibraryFormatFilter,
    onSortChange: (LibrarySortOption) -> Unit,
    onFilterChange: (LibraryFormatFilter) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            text = "书库",
            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
        )
        Row(
            modifier = Modifier.focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "排序",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            )
            LibrarySortOption.values().forEach { option ->
                FilterChip(
                    label = option.label,
                    selected = selectedSort == option,
                    onClick = { onSortChange(option) },
                )
            }
            Spacer(modifier = Modifier.width(20.dp))
            Text(
                text = "格式",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            )
            LibraryFormatFilter.values().forEach { filter ->
                FilterChip(
                    label = filter.label,
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                )
            }
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.26f),
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyLibrary(
    onGoTransfer: () -> Unit,
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
                text = "书库暂时为空",
                style = MaterialTheme.typography.titleLarge,
            )
            Button(onClick = onGoTransfer) {
                Text("前往传书")
            }
        }
    }
}

@Composable
private fun EmptyFilterResult() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 60.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "当前筛选下没有书籍",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "切换格式或排序后继续浏览书库",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            )
        }
    }
}
