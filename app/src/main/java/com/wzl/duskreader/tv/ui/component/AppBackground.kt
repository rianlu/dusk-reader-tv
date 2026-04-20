package com.wzl.duskreader.tv.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wzl.duskreader.tv.ui.theme.BackgroundPrimary

/**
 * Apple TV 标准背景
 * 纯黑背景,简洁干净
 */
@Composable
fun AppBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundPrimary),
        content = content
    )
}
