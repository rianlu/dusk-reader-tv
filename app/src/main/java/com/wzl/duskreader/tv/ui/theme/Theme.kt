package com.wzl.duskreader.tv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/**
 * Apple TV 标准主题
 * 纯黑背景 + 清晰的视觉层次
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun DuskReaderTVTheme(
    isInDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = TextPrimary,
        onPrimary = BackgroundPrimary,
        secondary = TextSecondary,
        onSecondary = BackgroundPrimary,
        background = BackgroundPrimary,
        surface = BackgroundSecondary,
        onSurface = TextPrimary,
        surfaceVariant = BackgroundTertiary,
        onSurfaceVariant = TextSecondary
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}