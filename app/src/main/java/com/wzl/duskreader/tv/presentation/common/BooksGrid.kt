@file:OptIn(
    androidx.tv.material3.ExperimentalTvMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.wzl.duskreader.tv.presentation.common

import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed as gridItemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRestorer
import androidx.tv.material3.MaterialTheme
import androidx.compose.ui.unit.dp
import com.wzl.duskreader.tv.data.entities.Book

/** 书库网格列数 */
const val BOOKS_GRID_COLUMNS = 5

private val BOOK_POSTER_ASPECT_RATIO = 3f / 4f

/**
 * TV pivot 滚动：把聚焦行钉在视口约 30% 处（对齐 TvMaterialCatalog 的
 * PositionFocusedItemInLazyLayout 实现公式）。默认策略下聚焦项贴边时下一行尚未组合，
 * D-pad 焦点搜索落空导致跳焦；pivot 让后续行提前组合。
 * 注意：不覆写 scrollAnimationSpec——默认弹簧动画支持连发按键时平滑重定向，
 * 自定义 tween 每次按键都从静止重启，快速连按时滚动追不上焦点。
 */
private const val BOOKS_GRID_PIVOT_PARENT_FRACTION = 0.3f

private val BooksGridPivotSpec = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
        val childSmallerThanParent = size <= containerSize
        val initialTargetForLeadingEdge = BOOKS_GRID_PIVOT_PARENT_FRACTION * containerSize
        val spaceAvailableToShowItem = containerSize - initialTargetForLeadingEdge
        val targetForLeadingEdge =
            if (childSmallerThanParent && spaceAvailableToShowItem < size) {
                containerSize - size
            } else {
                initialTargetForLeadingEdge
            }
        return offset - targetForLeadingEdge
    }
}

/**
 * 书库网格容器：焦点治理内收的可复用组件（对齐官方 MoviesRow 的容器级焦点模式）。
 *
 * - pivot 滚动（30%，默认弹簧动画，不自定义 tween）
 * - focusRestorer fallback 指向 [anchorRequester]：调用方传入一个恒组合的锚点
 *   （如 toolbar 按钮），焦点回到网格时优先恢复上次聚焦项，被回收时退到锚点，
 *   不会跳到任意已组合项
 * - per-item 规则最小化（对齐官方 CategoriesScreen）：仅封网格左/右边界防跨行逃逸，
 *   行内移动交给默认 2D 焦点搜索
 *
 * @param books 网格数据（key = book.id）
 * @param anchorRequester 恒组合的焦点恢复锚点（toolbar 搜索按钮等，不在 Lazy 容器内）
 * @param onBookClick 点击书籍（调用方导航到详情页）
 * @param bookTile 项内容渲染（默认 [BookTile]，可替换外观但焦点规则由容器统一治理）
 */
@Composable
fun BooksGrid(
    books: List<Book>,
    anchorRequester: FocusRequester,
    onBookClick: (Book) -> Unit,
    modifier: Modifier = Modifier,
    state: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(bottom = 132.dp),
    bookTile: @Composable (Book, Modifier) -> Unit = { book, tileModifier ->
        BookTile(book = book, modifier = tileModifier, onClick = { onBookClick(book) })
    },
) {
    CompositionLocalProvider(LocalBringIntoViewSpec provides BooksGridPivotSpec) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(BOOKS_GRID_COLUMNS),
            state = state,
            modifier = modifier
                .fillMaxSize()
                // restorer fallback 指向恒组合锚点，不指向 Lazy 项（回收后 requestFocus 会崩）
                .focusRestorer { anchorRequester },
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            gridItemsIndexed(books, key = { _, book -> book.id }) { index, book ->
                Box(
                    modifier = Modifier
                        // item 级重排/增删动画（foundation 1.7）：
                        // 搜索/筛选/排序切换时书位平滑过渡而非瞬变（官方 MoviesRow 用
                        // AnimatedContent 包行实现同目的，网格场景对应物是 animateItem）
                        .animateItem()
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp, vertical = 6.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    bookTile(
                        book,
                        Modifier
                            .fillMaxWidth()
                            .focusProperties {
                                if (index % BOOKS_GRID_COLUMNS == 0) {
                                    left = FocusRequester.Cancel
                                }
                                if (index % BOOKS_GRID_COLUMNS == BOOKS_GRID_COLUMNS - 1) {
                                    right = FocusRequester.Cancel
                                }
                            },
                    )
                }
            }
        }
    }
}

/**
 * 默认书格：海报 + 书名 + 进度条。聚焦签名遵循 DESIGN.md §2.2/§4
 * （白底反相 + 2dp 描边 + focusedScale 1f 浏览档）。
 */
@Composable
private fun BookTile(
    book: Book,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    androidx.tv.material3.Surface(
        onClick = onClick,
        modifier = modifier,
        shape = androidx.tv.material3.ClickableSurfaceDefaults.shape(MaterialTheme.shapes.large),
        colors = androidx.tv.material3.ClickableSurfaceDefaults.colors(
            containerColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.045f),
            focusedContainerColor = androidx.compose.ui.graphics.Color.White,
        ),
        border = androidx.tv.material3.ClickableSurfaceDefaults.border(
            border = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    androidx.compose.ui.graphics.Color.White.copy(alpha = 0.10f),
                ),
                shape = MaterialTheme.shapes.large,
            ),
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    androidx.compose.ui.graphics.Color.White,
                ),
                shape = MaterialTheme.shapes.large,
            ),
        ),
        scale = androidx.tv.material3.ClickableSurfaceDefaults.scale(focusedScale = 1f),
    ) {
        androidx.compose.foundation.layout.Column(
            modifier = Modifier.padding(6.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            BookCoverWithChip(book = book, modifier = Modifier.fillMaxWidth())
            // 书名与进度条由调用方更了解排版上下文，这里给默认实现
            androidx.tv.material3.Text(
                text = book.title,
                style = MaterialTheme.typography.titleSmall,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.86f),
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun BookCoverWithChip(
    book: Book,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.aspectRatio(BOOK_POSTER_ASPECT_RATIO)) {
        BookCover(book = book, modifier = Modifier.fillMaxSize())
    }
}
