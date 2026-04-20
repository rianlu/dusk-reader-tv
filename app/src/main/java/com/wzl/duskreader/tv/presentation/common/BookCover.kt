package com.wzl.duskreader.tv.presentation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wzl.duskreader.tv.data.entities.Book
import kotlin.math.absoluteValue

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
        PlaceholderCover(title = book.title, modifier = modifier)
    }
}

@Composable
private fun PlaceholderCover(title: String, modifier: Modifier) {
    Box(
        modifier = modifier.background(brushForTitle(title)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.firstOrNull()?.toString() ?: "?",
            color = Color.White,
            fontSize = 64.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

private val CoverPalette = listOf(
    listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
    listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
    listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
    listOf(Color(0xFF43E97B), Color(0xFF38F9D7)),
    listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
    listOf(Color(0xFF30CFD0), Color(0xFF330867)),
)

private fun brushForTitle(title: String): Brush {
    val idx = title.hashCode().absoluteValue % CoverPalette.size
    return Brush.linearGradient(CoverPalette[idx])
}
