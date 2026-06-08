package com.wzl.duskreader.tv.presentation.screens.bookDetails

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.SurfaceDefaults
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.hasReadingHistory
import com.wzl.duskreader.tv.data.entities.progressRatio
import com.wzl.duskreader.tv.presentation.common.BookCover
import com.wzl.duskreader.tv.presentation.common.DuskTvButton
import com.wzl.duskreader.tv.presentation.common.DuskTvButtonStyle
import com.wzl.duskreader.tv.presentation.common.Error
import com.wzl.duskreader.tv.presentation.common.Loading
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding
import java.util.Locale

object BookDetailsScreen {
    const val BookIdBundleKey = "bookId"
}

@Composable
fun BookDetailsScreen(
    onBackPressed: () -> Unit,
    onStartReading: (book: Book) -> Unit,
    viewModel: BookDetailsScreenViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is BookDetailsScreenUiState.Loading -> Loading(modifier = Modifier.fillMaxSize())
        is BookDetailsScreenUiState.Error -> Error(modifier = Modifier.fillMaxSize())
        is BookDetailsScreenUiState.Done -> Details(
            book = state.book,
            onBackPressed = onBackPressed,
            onStartReading = { onStartReading(state.book) },
            modifier = Modifier.fillMaxSize(),
        )
    }
}

@Composable
private fun Details(
    book: Book,
    onBackPressed: () -> Unit,
    onStartReading: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackPressed)
    val childPadding = rememberChildPadding()
    val startButtonFocus = remember { FocusRequester() }

    LaunchedEffect(Unit) { startButtonFocus.requestFocus() }

    Box(modifier = modifier.fillMaxSize().background(Color(0xFF05070B))) {
        DetailsBackground()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = childPadding.start,
                    end = childPadding.end,
                    top = 48.dp,
                    bottom = 48.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(38.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.06f)),
                shape = MaterialTheme.shapes.extraLarge,
                border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)), shape = MaterialTheme.shapes.extraLarge),
            ) {
                BookCover(
                    book = book,
                    modifier = Modifier
                        .padding(14.dp)
                        .width(248.dp)
                        .aspectRatio(10.5f / 16f),
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = book.format.uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.54f),
                )
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = book.author?.takeIf { it.isNotBlank() } ?: "未知作者",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                MetaRow(book = book)
                DescriptionPanel(book = book)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.focusGroup(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    DuskTvButton(
                        text = if (book.hasReadingHistory()) "继续阅读" else "开始阅读",
                        icon = Icons.AutoMirrored.Outlined.MenuBook,
                        modifier = Modifier.focusRequester(startButtonFocus),
                        onClick = onStartReading,
                    )
                    DuskTvButton(
                        text = "返回",
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        style = DuskTvButtonStyle.Secondary,
                        onClick = onBackPressed,
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailsBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.horizontalGradient(
                    colors = listOf(Color(0xFF0A0D13), Color(0xFF121B2D), Color(0xFF05070B)),
                ),
            ),
    )
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color.Black.copy(alpha = 0.10f), Color.Transparent, Color.Black.copy(alpha = 0.58f)),
                ),
            ),
    )
}

@Composable
private fun DescriptionPanel(book: Book) {
    Surface(
        modifier = Modifier.widthIn(max = 780.dp),
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.07f)),
        shape = MaterialTheme.shapes.large,
        border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), shape = MaterialTheme.shapes.large),
    ) {
        Text(
            text = book.description?.takeIf { it.isNotBlank() } ?: "这本书还没有简介. 打开后会自动记录阅读进度.",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.74f),
            maxLines = 5,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun MetaRow(book: Book) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        MetaChip(label = "大小", value = formatFileSize(book.fileSize))
        MetaChip(label = "进度", value = progressPercent(book) ?: "未开始")
        MetaChip(label = "来源", value = "本地书库")
    }
}

@Composable
private fun MetaChip(label: String, value: String) {
    Surface(
        colors = SurfaceDefaults.colors(containerColor = Color.White.copy(alpha = 0.08f)),
        shape = MaterialTheme.shapes.large,
        border = Border(BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)), shape = MaterialTheme.shapes.large),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Color.White.copy(alpha = 0.48f),
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
            )
        }
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "—"
    val kb = bytes / 1024.0
    if (kb < 1024) return String.format(Locale.ROOT, "%.1f KB", kb)
    val mb = kb / 1024.0
    return String.format(Locale.ROOT, "%.1f MB", mb)
}

private fun progressPercent(book: Book): String? {
    if (!book.hasReadingHistory()) return null
    val pct = (book.progressRatio() * 100).coerceIn(0f, 100f)
    return String.format(Locale.ROOT, "%.0f%%", pct)
}
