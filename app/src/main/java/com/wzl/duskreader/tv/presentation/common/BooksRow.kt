package com.wzl.duskreader.tv.presentation.common

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.BookList
import com.wzl.duskreader.tv.presentation.screens.dashboard.rememberChildPadding

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun BooksRow(
    bookList: BookList,
    modifier: Modifier = Modifier,
    startPadding: Dp = rememberChildPadding().start,
    endPadding: Dp = rememberChildPadding().end,
    title: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
    ),
    showItemTitle: Boolean = true,
    onBookSelected: (book: Book) -> Unit = {},
) {
    val (lazyRow, firstItem) = remember { FocusRequester.createRefs() }

    Column(modifier = modifier.focusGroup()) {
        if (title != null) {
            Text(
                text = title,
                style = titleStyle,
                modifier = Modifier
                    .alpha(1f)
                    .padding(start = startPadding, top = 16.dp, bottom = 16.dp),
            )
        }
        AnimatedContent(
            targetState = bookList,
            label = "",
        ) { bookState ->
            LazyRow(
                contentPadding = PaddingValues(start = startPadding, end = endPadding),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier
                    .focusRequester(lazyRow)
                    .focusRestorer { firstItem },
            ) {
                itemsIndexed(bookState, key = { _, book -> book.id }) { index, book ->
                    val itemModifier = if (index == 0) {
                        Modifier.focusRequester(firstItem)
                    } else {
                        Modifier
                    }
                    BooksRowItem(
                        modifier = itemModifier.weight(1f),
                        index = index,
                        onBookSelected = {
                            lazyRow.saveFocusedChild()
                            onBookSelected(it)
                        },
                        book = book,
                        showItemTitle = showItemTitle,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BooksRowItem(
    index: Int,
    book: Book,
    onBookSelected: (Book) -> Unit,
    showItemTitle: Boolean,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }

    BookCard(
        onClick = { onBookSelected(book) },
        title = {
            BooksRowItemText(
                showItemTitle = showItemTitle,
                isItemFocused = isFocused,
                book = book,
            )
        },
        modifier = Modifier
            .onFocusChanged { isFocused = it.isFocused }
            .focusProperties {
                left = if (index == 0) FocusRequester.Cancel else FocusRequester.Default
            }
            .then(modifier),
    ) {
        Box(contentAlignment = Alignment.CenterStart) {
            BookCover(
                book = book,
                modifier = Modifier
                    .aspectRatio(10.5f / 16f)
                    .fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun BooksRowItemText(
    showItemTitle: Boolean,
    isItemFocused: Boolean,
    book: Book,
    modifier: Modifier = Modifier,
) {
    if (showItemTitle) {
        val titleAlpha by animateFloatAsState(
            targetValue = if (isItemFocused) 1f else 0f,
            label = "",
        )
        Text(
            text = book.title,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold,
            ),
            textAlign = TextAlign.Center,
            modifier = modifier
                .alpha(titleAlpha)
                .fillMaxWidth()
                .padding(top = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
