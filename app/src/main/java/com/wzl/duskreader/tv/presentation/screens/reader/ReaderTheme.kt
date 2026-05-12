package com.wzl.duskreader.tv.presentation.screens.reader

import androidx.compose.ui.graphics.Color

enum class ReaderTheme(
    val bgColor: Color,
    val textColor: Color,
    val displayName: String,
) {
    ForestNight(Color(0xFF101E19), Color(0xFFDCE6D8), "墨绿夜读"),
    CinemaGray(Color(0xFF15171A), Color(0xFFD8D5CC), "影院暗灰"),
    WarmParchment(Color(0xFFD8C9AC), Color(0xFF2F281E), "暖纸柔光"),
    HighContrast(Color(0xFF050607), Color(0xFFF0F0E8), "高对比"),
}

enum class ReaderTextBrightness(
    val displayName: String,
    val multiplier: Float,
) {
    Soft("柔和", 0.88f),
    Standard("标准", 1.0f),
    Clear("清晰", 1.12f),
}

fun ReaderTheme.adjustedTextColor(brightness: ReaderTextBrightness): Color {
    return textColor.scaleRgb(brightness.multiplier)
}

private fun Color.scaleRgb(multiplier: Float): Color {
    return Color(
        red = (red * multiplier).coerceIn(0f, 1f),
        green = (green * multiplier).coerceIn(0f, 1f),
        blue = (blue * multiplier).coerceIn(0f, 1f),
        alpha = alpha,
    )
}
