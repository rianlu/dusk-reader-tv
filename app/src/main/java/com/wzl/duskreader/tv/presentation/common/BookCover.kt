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
        // 无封面图：只显示渐变占位，标题由调用方在封面下方独立显示，
        // 避免标题重复出现（一次在封面里、一次在标题行）。
        DefaultBookCover(modifier = modifier)
    }
}

@Composable
private fun DefaultBookCover(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            Color(0xFF11141B),
                            Color(0xFF1C2433),
                            Color(0xFF090C12),
                        ),
                        start = Offset(0f, Float.POSITIVE_INFINITY),
                        end = Offset(Float.POSITIVE_INFINITY, 0f),
                    ),
                ),
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
    }
}
