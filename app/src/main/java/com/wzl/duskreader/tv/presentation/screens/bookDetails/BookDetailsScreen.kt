package com.wzl.duskreader.tv.presentation.screens.bookDetails

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.hasReadingHistory
import com.wzl.duskreader.tv.data.entities.progressRatio
import com.wzl.duskreader.tv.presentation.common.BookCover
import com.wzl.duskreader.tv.presentation.common.DuskTvButton
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

    when (val s = uiState) {
        is BookDetailsScreenUiState.Loading -> Loading(modifier = Modifier.fillMaxSize())
        is BookDetailsScreenUiState.Error -> Error(modifier = Modifier.fillMaxSize())
        is BookDetailsScreenUiState.Done -> {
            Details(
                book = s.book,
                onBackPressed = onBackPressed,
                onStartReading = { onStartReading(s.book) },
                modifier = Modifier.fillMaxSize(),
            )
        }
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

    LaunchedEffect(Unit) {
        startButtonFocus.requestFocus()
    }

    Box(modifier = modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0xFF0A0D13),
                        0.5f to Color(0xFF121926),
                        1f to Color(0xFF07090E),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.82f),
                            Color.Black.copy(alpha = 0.56f),
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
                            Color.Black.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.66f),
                        ),
                    ),
                ),
        )
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = childPadding.start,
                    end = childPadding.end,
                    top = 48.dp,
                    bottom = childPadding.bottom,
                ),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(
                book = book,
                modifier = Modifier
                    .width(260.dp)
                    .aspectRatio(10.5f / 16f),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = book.title,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = Color.White,
                    maxLines = 2,
                )
                if (!book.author.isNullOrBlank()) {
                    Text(
                        text = book.author,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White.copy(alpha = 0.76f),
                    )
                }
                MetaRow(book = book)
                if (!book.description.isNullOrBlank()) {
                    Text(
                        text = book.description,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                        color = Color.White.copy(alpha = 0.82f),
                        maxLines = 5,
                    )
                }
                Spacer(Modifier.height(8.dp))
                StartReadingButton(
                    modifier = Modifier.focusRequester(startButtonFocus),
                    hasProgress = book.hasReadingHistory(),
                    onClick = onStartReading,
                )
            }
        }
    }
}

@Composable
private fun MetaRow(book: Book) {
    Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
        MetaItem(label = "格式", value = book.format.uppercase(Locale.ROOT))
        MetaItem(label = "大小", value = formatFileSize(book.fileSize))
        progressPercent(book)?.let { MetaItem(label = "进度", value = it) }
        MetaItem(label = "导入", value = "本地书库")
    }
}

@Composable
private fun MetaItem(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.52f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = Color.White,
        )
    }
}

@Composable
private fun StartReadingButton(
    modifier: Modifier = Modifier,
    hasProgress: Boolean,
    onClick: () -> Unit,
) {
    DuskTvButton(
        text = if (hasProgress) "继续阅读" else "开始阅读",
        icon = Icons.AutoMirrored.Outlined.MenuBook,
        modifier = modifier,
        contentDescription = null,
        onClick = onClick,
    )
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
