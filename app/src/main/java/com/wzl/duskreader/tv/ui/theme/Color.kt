package com.wzl.duskreader.tv.ui.theme

import androidx.compose.ui.graphics.Color

// Apple TV 标准色彩系统 - 纯黑背景 + 清晰层次

// 背景层次
val BackgroundPrimary = Color(0xFF000000)      // 纯黑背景
val BackgroundSecondary = Color(0xFF0A0A0A)    // 卡片背景
val BackgroundTertiary = Color(0xFF141414)     // 悬浮层背景

// 文字
val TextPrimary = Color(0xFFFFFFFF)            // 主要文字
val TextSecondary = Color(0xFFB3B3B3)          // 次要文字
val TextTertiary = Color(0xFF808080)           // 辅助文字

// 焦点
val FocusIndicator = Color(0xFFFFFFFF)         // 焦点边框
val FocusGlow = Color(0x33FFFFFF)              // 焦点光晕
val FocusBackground = Color(0x1AFFFFFF)        // 焦点背景

// 强调色
val AccentBlue = Color(0xFF0A84FF)             // 蓝色强调
val AccentGreen = Color(0xFF30D158)            // 成功状态
val AccentRed = Color(0xFFFF453A)              // 错误状态

// 旧色彩系统 (保留兼容)
val CinematicBackground = BackgroundPrimary
val CinematicSurface = BackgroundSecondary
val CinematicSurfaceLow = Color(0xFF1B1B1B)
val CinematicSurfaceBright = Color(0xFF393939)
val CinematicOnSurfaceVariant = TextSecondary
val CinematicPrimary = TextPrimary
val CinematicPrimaryContainer = Color(0xFFD4D4D4)
val CinematicOutlineVariant = Color(0xFF474747)
val CinematicGlass = Color(0x66393939)
val CinematicAccent = AccentBlue
val CinematicSuccess = AccentGreen
val CinematicWarning = Color(0xFFFFA726)
val CinematicError = AccentRed

// 渐变色彩
val GradientStart = Color(0xFF202636)
val GradientMid = Color(0xFF0F1218)
val GradientEnd = Color(0xFF090B0F)

// 焦点和光晕效果
val FocusRim = Color(0x55FFFFFF)
val ShadowAmbient = Color(0x88000000)
val ShadowSpot = Color(0xAA000000)