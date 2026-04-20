package com.wzl.duskreader.tv.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.itemsIndexed
import androidx.tv.material3.Button
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.data.model.Book
import com.wzl.duskreader.tv.data.model.hasReadingHistory
import com.wzl.duskreader.tv.data.model.progressRatio
import com.wzl.duskreader.tv.ui.component.BookCard
import com.wzl.duskreader.tv.ui.theme.AccentBlue
import com.wzl.duskreader.tv.ui.theme.BackgroundPrimary
import com.wzl.duskreader.tv.ui.theme.TextPrimary
import com.wzl.duskreader.tv.ui.theme.TextSecondary
import com.wzl.duskreader.tv.ui.theme.TextTertiary
import com.wzl.duskreader.tv.ui.theme.rememberDimensions
import com.wzl.duskreader.tv.ui.viewmodel.ShelfViewModel
import kotlin.math.absoluteValue

/**
 * 海报墙色板：每本书取一个深色调，配合大首字做装饰背景。
 */
private val BookAccents = listOf(
    Color(0xFF1F3B6B),  // 深蓝
    Color(0xFF4A1F6B),  // 深紫
    Color(0xFF1F4A4A),  // 深青
    Color(0xFF6B3020),  // 深赭
    Color(0xFF2D5B2D),  // 深绿
    Color(0xFF5B4020)   // 深棕
)

private fun Book.accentColor(): Color {
    val hash = (title.hashCode().absoluteValue + id.toInt().absoluteValue)
    return BookAccents[hash % BookAccents.size]
}

/**
 * 主页：沉浸式海报墙 + 底部最近阅读 Rail（参考 JetStream Top10MoviesList）。
 *
 * 焦点：
 * - entry 挂在 Rail 第一本
 * - 所有卡片 up=tabUpRequester、down=Cancel
 * - 第一本 left=Cancel
 */
@Composable
fun HomeScreen(
    viewModel: ShelfViewModel,
    entryRequester: FocusRequester,
    tabUpRequester: FocusRequester,
    onBookClick: (Book) -> Unit,
    onNavigateToTransfer: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimensions = rememberDimensions()
    val state by viewModel.homeScreenState.collectAsState()

    val displayBooks = remember(state) {
        (listOfNotNull(state.continueReading) + state.recentBooks)
            .ifEmpty { state.shelfBooks }
    }

    if (displayBooks.isEmpty()) {
        EmptyHomeScreen(
            modifier = modifier,
            entryRequester = entryRequester,
            tabUpRequester = tabUpRequester,
            onNavigateToTransfer = onNavigateToTransfer
        )
        return
    }

    var selectedBook by remember(displayBooks.firstOrNull()?.id) {
        mutableStateOf(displayBooks.first())
    }
    var isRailFocused by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = selectedBook,
            animationSpec = tween(600),
            label = "hero-background"
        ) { book ->
            BookHeroBackground(book = book, modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = dimensions.contentPadding),
            verticalArrangement = Arrangement.Bottom
        ) {
            AnimatedVisibility(
                visible = isRailFocused,
                enter = fadeIn(tween(300)),
                exit = fadeOut(tween(300))
            ) {
                BookMetaPanel(
                    book = selectedBook,
                    modifier = Modifier.padding(bottom = dimensions.spacingL)
                )
            }

            Text(
                text = "最近阅读",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(
                    start = dimensions.spacingXS,
                    bottom = dimensions.spacingS
                )
            )

            TvLazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRestorer()
                    .onFocusChanged { isRailFocused = it.hasFocus }
                    .padding(bottom = dimensions.contentPadding),
                contentPadding = PaddingValues(horizontal = dimensions.spacingXS),
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingL)
            ) {
                itemsIndexed(displayBooks) { index, book ->
                    val cardModifier = when (index) {
                        0 -> Modifier
                            .focusRequester(entryRequester)
                            .focusProperties {
                                left = FocusRequester.Cancel
                                up = tabUpRequester
                                down = FocusRequester.Cancel
                            }
                        else -> Modifier.focusProperties {
                            up = tabUpRequester
                            down = FocusRequester.Cancel
                        }
                    }
                    BookCard(
                        title = book.title,
                        progress = book.progressRatio(),
                        onClick = { onBookClick(book) },
                        modifier = cardModifier,
                        onFocused = { selectedBook = book }
                    )
                }
            }
        }
    }
}

@Composable
private fun BookHeroBackground(book: Book, modifier: Modifier = Modifier) {
    val accent = book.accentColor()
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            accent.copy(alpha = 0.72f),
                            accent.copy(alpha = 0.34f),
                            BackgroundPrimary
                        )
                    )
                )
        )

        Text(
            text = book.title.take(1),
            style = TextStyle(
                fontSize = 420.sp,
                fontWeight = FontWeight.Black,
                color = Color.White.copy(alpha = 0.08f)
            ),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 60.dp)
        )

        // 底部暗化，保证 Rail 与元信息可读
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.92f)
                        )
                    )
                )
        )
    }
}

@Composable
private fun BookMetaPanel(book: Book, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            colors = SurfaceDefaults.colors(containerColor = AccentBlue),
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = if (book.hasReadingHistory()) "继续阅读" else "开始阅读",
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
            )
        }

        Text(
            text = book.title,
            style = MaterialTheme.typography.displayMedium.copy(
                fontWeight = FontWeight.Bold,
                shadow = Shadow(color = Color.Black.copy(alpha = 0.6f), blurRadius = 6f)
            ),
            color = Color.White,
            maxLines = 1
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            MetaChip(book.format)
            book.author?.let { MetaChip(it) }
            if (book.hasReadingHistory()) {
                MetaChip("已读 ${(book.progressRatio() * 100).toInt()}%")
            }
        }
    }
}

@Composable
private fun MetaChip(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.78f)
    )
}

@Composable
private fun EmptyHomeScreen(
    modifier: Modifier,
    entryRequester: FocusRequester,
    tabUpRequester: FocusRequester,
    onNavigateToTransfer: () -> Unit
) {
    val dimensions = rememberDimensions()
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.QrCode,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = TextTertiary
        )
        Spacer(modifier = Modifier.height(dimensions.spacingL))
        Text(
            text = "还没有书籍",
            style = MaterialTheme.typography.displayMedium.copy(fontWeight = FontWeight.Bold),
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(dimensions.spacingS))
        Text(
            text = "使用传书功能将书籍导入到书库",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(0.6f)
        )
        Spacer(modifier = Modifier.height(dimensions.spacingXL))
        Button(
            onClick = onNavigateToTransfer,
            modifier = Modifier
                .width(240.dp)
                .height(64.dp)
                .focusRequester(entryRequester)
                .focusProperties { up = tabUpRequester }
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimensions.spacingS),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(24.dp))
                Text(
                    "前往传书",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}
