package com.wzl.duskreader.tv.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.Refresh
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
import com.wzl.duskreader.tv.util.DebugLogger

/**
 * 书架主页面：展示所有已导入的书籍。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ShelfScreen(
    viewModel: ShelfViewModel,
    onBookClick: (Book) -> Unit
) {
    val books by viewModel.books.collectAsState()
    
    DebugLogger.d("ShelfScreen", "重绘书架。书籍数量: ${books.size}")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 60.dp) // 提升至 60dp 安全区域
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "我的书架",
                style = MaterialTheme.typography.displaySmall
            )
            
            // 扫描按钮
            Button(onClick = { viewModel.scanStorage() }) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("扫描本地")
            }
        }

        if (books.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Inbox,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.Gray
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "书架空空如也，请点击左侧“传书”从手机导入",
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
                items(books) { book ->
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
