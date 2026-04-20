package com.wzl.duskreader.tv.presentation.screens.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text

object ReaderScreen {
    const val BookIdBundleKey = "bookId"
}

@Composable
fun ReaderScreen(
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(onBack = onBackPressed)
    Box(
        modifier = modifier
            .fillMaxSize()
            .focusable(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "阅读器",
                style = MaterialTheme.typography.displaySmall,
            )
            Text(
                text = "Phase 2C 接入：TxtReaderEngine + 翻页 + 进度保存",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
