package com.wzl.duskreader.tv.presentation.screens.bookDetails

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.presentation.common.BookCover
import com.wzl.duskreader.tv.presentation.common.Error
import com.wzl.duskreader.tv.presentation.common.Loading
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding
import com.wzl.duskreader.tv.presentation.theme.JetStreamButtonShape
import java.util.Locale
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember

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

    Row(
        modifier = modifier
            .padding(
                start = childPadding.start,
                end = childPadding.end,
                top = 48.dp,
                bottom = childPadding.bottom,
            ),
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        BookCover(
            book = book,
            modifier = Modifier
                .width(240.dp)
                .aspectRatio(10.5f / 16f),
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Bold,
                ),
                maxLines = 2,
            )
            if (!book.author.isNullOrBlank()) {
                Text(
                    text = "作者：${book.author}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .alpha(0.75f),
                )
            }
            Spacer(Modifier.height(20.dp))
            MetaRow(book = book)
            if (!book.description.isNullOrBlank()) {
                Text(
                    text = book.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    modifier = Modifier
                        .padding(top = 24.dp)
                        .alpha(0.85f),
                    maxLines = 6,
                )
            }
            Spacer(Modifier.height(32.dp))
            StartReadingButton(
                modifier = Modifier.focusRequester(startButtonFocus),
                hasProgress = book.lastReadPosition > 0,
                onClick = onStartReading,
            )
        }
    }
}

@Composable
private fun MetaRow(book: Book) {
    Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
        MetaItem(label = "格式", value = book.format.uppercase(Locale.ROOT))
        MetaItem(label = "大小", value = formatFileSize(book.fileSize))
        val progress = progressPercent(book)
        if (progress != null) {
            MetaItem(label = "进度", value = progress)
        }
    }
}

@Composable
private fun MetaItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.alpha(0.6f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun StartReadingButton(
    modifier: Modifier = Modifier,
    hasProgress: Boolean,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
        shape = ButtonDefaults.shape(shape = JetStreamButtonShape),
    ) {
        Icon(imageVector = Icons.AutoMirrored.Outlined.MenuBook, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text(
            text = if (hasProgress) "继续阅读" else "开始阅读",
            style = MaterialTheme.typography.titleSmall,
        )
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
    val total = book.totalSize
    if (total <= 0L) return null
    if (book.lastReadPosition <= 0) return null
    val pct = (book.lastReadPosition.toDouble() / total.toDouble() * 100).coerceIn(0.0, 100.0)
    return String.format(Locale.ROOT, "%.0f%%", pct)
}
