package com.wzl.duskreader.tv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 响应式尺寸系统
 * 根据屏幕宽度动态调整 UI 元素大小
 *
 * 基准: 1920px 宽度
 * 小屏 (< 1280): 0.7x
 * 中屏 (1280-1920): 0.85x
 * 大屏 (1920-2560): 1.0x
 * 超大屏 (> 2560): 1.15x
 */
data class Dimensions(
    val scale: Float,

    // 间距
    val spacingXS: Dp,
    val spacingS: Dp,
    val spacingM: Dp,
    val spacingL: Dp,
    val spacingXL: Dp,

    // 侧边栏
    val sidebarWidth: Dp,
    val sidebarItemHeight: Dp,
    val sidebarPadding: Dp,

    // 卡片
    val bookCardWidth: Dp,
    val bookCardAspectRatio: Float,
    val cardSpacing: Dp,
    val cardCornerRadius: Dp,

    // Hero 卡片
    val heroCardHeight: Dp,
    val heroCardCornerRadius: Dp,

    // 内容区域
    val contentPadding: Dp,
    val railSpacing: Dp,

    // 焦点
    val focusBorderWidth: Dp,
    val focusShadowElevation: Dp,

    // 阅读器
    val readerHorizontalPadding: Dp,
    val readerVerticalPadding: Dp,
    val readerColumnGap: Dp
) {
    companion object {
        @Composable
        fun calculate(): Dimensions {
            val configuration = LocalConfiguration.current
            val screenWidth = configuration.screenWidthDp

            return remember(screenWidth) {
                val scale = when {
                    screenWidth < 1280 -> 0.7f
                    screenWidth < 1920 -> 0.85f
                    screenWidth < 2560 -> 1.0f
                    else -> 1.15f
                }

                Dimensions(
                    scale = scale,

                    // 间距
                    spacingXS = (6 * scale).dp,
                    spacingS = (12 * scale).dp,
                    spacingM = (18 * scale).dp,
                    spacingL = (24 * scale).dp,
                    spacingXL = (36 * scale).dp,

                    // 侧边栏
                    sidebarWidth = (160 * scale).dp,
                    sidebarItemHeight = (56 * scale).dp,
                    sidebarPadding = (16 * scale).dp,

                    // 卡片
                    bookCardWidth = (140 * scale).dp,
                    bookCardAspectRatio = 0.7f,
                    cardSpacing = (16 * scale).dp,
                    cardCornerRadius = (12 * scale).dp,

                    // Hero 卡片 - 高度固定 360dp，不随屏幕缩放（设计规范）
                    heroCardHeight = 360.dp,
                    heroCardCornerRadius = (16 * scale).dp,

                    // 内容区域
                    contentPadding = (48 * scale).dp,
                    railSpacing = (32 * scale).dp,

                    // 焦点
                    focusBorderWidth = (2 * scale).dp,
                    focusShadowElevation = (24 * scale).dp,

                    // 阅读器
                    readerHorizontalPadding = (80 * scale).dp,
                    readerVerticalPadding = (60 * scale).dp,
                    readerColumnGap = (60 * scale).dp
                )
            }
        }
    }
}

/**
 * 提供当前尺寸的 Composable
 */
@Composable
fun rememberDimensions(): Dimensions {
    return Dimensions.calculate()
}
