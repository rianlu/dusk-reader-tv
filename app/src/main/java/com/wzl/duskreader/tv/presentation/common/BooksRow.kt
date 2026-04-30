package com.wzl.duskreader.tv.presentation.common

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
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
    titleStyle: TextStyle = MaterialTheme.typography.headlineSmall.copy(
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
    ),
    onBookSelected: (book: Book) -> Unit = {},
) {
    val (lazyRow, firstItem) = remember { FocusRequester.createRefs() }

    Column(modifier = modifier.focusGroup()) {
        if (title != null) {
            Text(
                text = title,
                style = titleStyle,
                modifier = Modifier.padding(start = startPadding, bottom = 14.dp),
            )
        }
        LazyRow(
            contentPadding = PaddingValues(start = startPadding, end = endPadding),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .focusRequester(lazyRow)
                .focusRestorer { firstItem },
        ) {
            itemsIndexed(bookList, key = { _, book -> book.id }) { index, book ->
                val itemModifier = if (index == 0) {
                    Modifier.focusRequester(firstItem)
                } else {
                    Modifier
                }
                BookCard(
                    modifier = itemModifier
                        .width(184.dp)
                        .focusProperties {
                            left = if (index == 0) FocusRequester.Cancel else FocusRequester.Default
                        },
                    onClick = { onBookSelected(book) },
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
        }
    }
}
