package com.wzl.duskreader.tv.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.itemsIndexed
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.data.model.progressRatio
import com.wzl.duskreader.tv.ui.component.BookCard
import com.wzl.duskreader.tv.ui.component.SectionHeader
import com.wzl.duskreader.tv.ui.theme.rememberDimensions
import com.wzl.duskreader.tv.ui.viewmodel.ShelfViewModel

private const val COLUMNS_PER_ROW = 6

/**
 * 书架：全部书籍按 [COLUMNS_PER_ROW] 列网格展示。
 *
 * 焦点：
 * - 第一本挂 [entryRequester]，up=tabUpRequester，左=Cancel
 * - 每行最左一本 left=Cancel，避免左键跳出 Row
 * - 最后一行没有下邻居，Compose 默认停留；上下由 TvLazyColumn 处理滚动
 */
@Composable
fun ShelfScreen(
    viewModel: ShelfViewModel,
    entryRequester: FocusRequester,
    tabUpRequester: FocusRequester,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = rememberDimensions()
    val books by viewModel.books.collectAsState()

    val rows = books.chunked(COLUMNS_PER_ROW)

    TvLazyColumn(
        modifier = modifier
            .fillMaxSize()
            .focusRestorer(),
        contentPadding = PaddingValues(
            start = dimensions.contentPadding,
            end = dimensions.contentPadding,
            top = dimensions.spacingM,
            bottom = dimensions.contentPadding
        ),
        verticalArrangement = Arrangement.spacedBy(dimensions.spacingL)
    ) {
        item {
            SectionHeader(
                title = "书架",
                subtitle = "共 ${books.size} 本"
            )
        }

        itemsIndexed(rows) { rowIndex, rowBooks ->
            Row(horizontalArrangement = Arrangement.spacedBy(dimensions.spacingL)) {
                rowBooks.forEachIndexed { colIndex, book ->
                    val isFirst = rowIndex == 0 && colIndex == 0
                    val isRowStart = colIndex == 0
                    val cardModifier = when {
                        isFirst -> Modifier
                            .focusRequester(entryRequester)
                            .focusProperties {
                                up = tabUpRequester
                                left = FocusRequester.Cancel
                            }
                        isRowStart -> Modifier.focusProperties { left = FocusRequester.Cancel }
                        else -> Modifier
                    }
                    BookCard(
                        title = book.title,
                        progress = book.progressRatio(),
                        onClick = { onBookClick(book) },
                        modifier = cardModifier
                    )
                }
            }
        }
    }
}
