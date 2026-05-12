package com.wzl.duskreader.tv.presentation.screens.reader

/**
 * 阅读器翻页方式。
 *
 * - [HORIZONTAL] 左右方向键翻页（默认，等同 1.0 行为）
 * - [VERTICAL] 上下方向键翻页
 * - [AUTO] 定时自动翻页，间隔由 ReaderScreen 内的 autoTurnSeconds 控制
 */
enum class PageTurnMode(val displayName: String) {
    HORIZONTAL("左右键"),
    VERTICAL("上下键"),
    AUTO("自动翻页"),
}

object AutoTurnInterval {
    const val MIN_SECONDS = 5
    const val MAX_SECONDS = 60
    const val STEP_SECONDS = 5
    const val DEFAULT_SECONDS = 10

    /** 减号：先把当前值向下对齐到 5 的倍数；若已对齐则减一档；最后 coerce 到 MIN。 */
    fun decrement(current: Int): Int {
        val aligned = (current / STEP_SECONDS) * STEP_SECONDS
        val next = if (aligned == current) current - STEP_SECONDS else aligned
        return next.coerceAtLeast(MIN_SECONDS)
    }

    /** 加号：先把当前值向上对齐到 5 的倍数；若已对齐则加一档；最后 coerce 到 MAX。 */
    fun increment(current: Int): Int {
        val aligned = ((current + STEP_SECONDS - 1) / STEP_SECONDS) * STEP_SECONDS
        val next = if (aligned == current) current + STEP_SECONDS else aligned
        return next.coerceAtMost(MAX_SECONDS)
    }
}
