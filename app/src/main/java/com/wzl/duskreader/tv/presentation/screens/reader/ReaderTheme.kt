package com.wzl.duskreader.tv.presentation.screens.reader

import androidx.compose.ui.graphics.Color

enum class ReaderTheme(
    val bgColor: Color,
    val textColor: Color,
    val displayName: String,
) {
    Parchment(Color(0xFFF5F2E9), Color(0xFF2C2C2C), "羊皮纸"),
    Cinematic(Color(0xFF0E0E0E), Color(0xFFC6C6C6), "沉浸黑"),
    DeepSea(Color(0xFF1A1C2C), Color(0xFFA0A5B1), "深海"),
}
