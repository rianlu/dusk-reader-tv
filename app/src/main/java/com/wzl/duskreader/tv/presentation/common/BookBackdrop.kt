package com.wzl.duskreader.tv.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.wzl.duskreader.tv.R
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
        Image(
            painter = painterResource(id = R.drawable.default_book_backdrop),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}
