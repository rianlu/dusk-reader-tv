package com.wzl.duskreader.tv.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wzl.duskreader.tv.data.entities.Book
import com.wzl.duskreader.tv.data.entities.preferredBackdropPath

@Composable
fun BookBackdrop(
    book: Book,
    modifier: Modifier = Modifier,
) {
    val path = book.preferredBackdropPath()
    if (!path.isNullOrBlank()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(path)
                .crossfade(true)
                .build(),
            contentDescription = book.title,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    } else {
        DefaultBackdrop(modifier = modifier)
    }
}

@Composable
private fun DefaultBackdrop(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(
                Brush.horizontalGradient(
                    0f to Color(0xFF0A0D13),
                    0.5f to Color(0xFF121926),
                    1f to Color(0xFF07090E),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.08f),
                            Color.Black.copy(alpha = 0.16f),
                        ),
                        start = Offset(0f, Float.POSITIVE_INFINITY),
                        end = Offset(Float.POSITIVE_INFINITY, 0f),
                    ),
                ),
        )
    }
}
