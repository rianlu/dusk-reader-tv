package com.wzl.duskreader.tv.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.*
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.ui.component.BookCard
import com.wzl.duskreader.tv.ui.viewmodel.ShelfViewModel

/**
 * 最近阅读页面：展示最近打开过的 5 本书籍。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RecentScreen(
    viewModel: ShelfViewModel,
    onBookClick: (Book) -> Unit
) {
    val recentBooks by viewModel.recentBooks.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "最近阅读",
                style = MaterialTheme.typography.displaySmall
            )
        }

        if (recentBooks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "还没有任何阅读历史，快去书架找本书看吧",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Adaptive(160.dp),
                contentPadding = PaddingValues(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                items(recentBooks) { book ->
                    BookCard(
                        title = book.title,
                        onClick = { onBookClick(book) },
                        onLongClick = { viewModel.removeBook(book) }
                    )
                }
            }
        }
    }
}
