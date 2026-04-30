package com.wzl.duskreader.tv.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wzl.duskreader.tv.R
import com.wzl.duskreader.tv.data.entities.Book

@Composable
fun BookCover(
    book: Book,
    modifier: Modifier = Modifier,
) {
    val coverPath = book.coverPath
    if (!coverPath.isNullOrBlank()) {
        AsyncImage(
            modifier = modifier,
            model = ImageRequest.Builder(LocalContext.current)
                .crossfade(true)
                .data(coverPath)
                .build(),
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
        )
    } else {
        DefaultBookCover(book = book, modifier = modifier)
    }
}

@Composable
private fun DefaultBookCover(
    book: Book,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.BottomStart,
    ) {
        Image(
            painter = painterResource(id = R.drawable.default_book_cover),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.18f),
                            Color.Black.copy(alpha = 0.72f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            if (!book.author.isNullOrBlank()) {
                Text(
                    text = book.author,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    modifier = Modifier.padding(top = 6.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
