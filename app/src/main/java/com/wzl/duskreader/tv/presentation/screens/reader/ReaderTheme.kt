package com.wzl.duskreader.tv.presentation.screens.reader

import androidx.compose.ui.graphics.Color

enum class ReaderTheme(
    val bgColor: Color,
    val textColor: Color,
    val displayName: String,
) {
    NightBlack(Color(0xFF0A0A0A), Color(0xFFE4E0D8), "深色黑底"),
    WarmParchment(Color(0xFFF4E9D8), Color(0xFF302A24), "护眼米色"),
    PaperGray(Color(0xFFE6E6E1), Color(0xFF2D2D2D), "浅灰纸面"),
    ForestNight(Color(0xFF13241E), Color(0xFFD6E2D9), "墨绿色夜读"),
}
