package com.wzl.duskreader.tv.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * 暮阅色彩常量（DESIGN.md §2 的代码沉淀）。
 *
 * 全 App 交互语言 = 白 ↔ 近黑反相（见 DESIGN.md §2.1/2.2）：
 * - 聚焦签名：白底 + 黑内容 + 2dp 白描边
 * - 选中（未聚焦）：White @ 0.14~0.16 轻填充
 * - 面板/小卡静息：近黑阶或低透明白
 *
 * 新代码一律取用本对象，不再手写 `Color.White.copy(alpha = …)`；
 * 旧代码渐进迁移（每次改动所触碰的文件顺手替换）。
 * 阶梯取值以 DESIGN.md §2.4 为准，超出阶梯的值先归档进 [alpha] 再使用。
 */
object DuskColors {

    // ---------- 聚焦签名（§2.2，全局唯一） ----------
    val FocusContainer = Color.White
    val FocusContent = Color.Black
    val FocusBorder = Color.White

    // ---------- 选中态（未聚焦，§2.2） ----------
    val SelectedContainer = Color.White.copy(alpha = 0.15f)
    val SelectedContent = Color.White

    // ---------- 白 alpha 阶梯（§2.4，九级 + 归档值） ----------
    // 文字/图标层：主 1.0 → 次 0.92 → 0.82 → 0.72 → 0.66 → 0.58/0.56 → 0.50 → 0.42
    val TextPrimary = Color.White
    val TextHigh = Color.White.copy(alpha = 0.92f)
    val TextStrong = Color.White.copy(alpha = 0.86f)
    val TextMedium = Color.White.copy(alpha = 0.72f)
    val TextSoft = Color.White.copy(alpha = 0.66f)
    val TextMuted = Color.White.copy(alpha = 0.58f)
    val TextFaint = Color.White.copy(alpha = 0.50f)

    // 静息填充层（面板/小卡/描边）
    val PanelContainer = Color.White.copy(alpha = 0.07f)       // 浏览页 PrimaryPanel
    val PanelBorder = Color.White.copy(alpha = 0.12f)          // 面板描边
    val CardContainer = Color.White.copy(alpha = 0.05f)        // 小卡/按钮静息
    val CardContainerSubtle = Color.White.copy(alpha = 0.045f) // 网格 tile 静息
    val CardBorder = Color.White.copy(alpha = 0.14f)           // 小卡描边
    val BorderResting = Color.White.copy(alpha = 0.10f)        // 通用静息描边
    val IconBackdrop = Color.White.copy(alpha = 0.10f)         // 图标底衬
    val OverlayDeep = Color.Black.copy(alpha = 0.18f)          // 地址块等深衬底

    // ---------- 近黑铬层（§2.3，阅读页资产） ----------
    val ChromePanel = Color(0xFF111111)   // 阅读设置抽屉
    val ChromePanelAlt = Color(0xFF171717) // 阅读目录抽屉
    val ChromeCard = Color(0xFF222222)    // 静息选项卡

    // ---------- 浏览页背景（§2.3） ----------
    val PageBackground = Color(0xFF070D15)
    val PageBackgroundDeep = Color(0xFF05070B) // 详情页舞台底
    val GlowAccent = Color(0xFF17263A)    // 径向氛围光

    // ---------- 格式强调色（§2.6，仅非交互处） ----------
    val AccentTxt = Color(0xFFFBBF24)
    val AccentEpub = Color(0xFF7DD3FC)
}
