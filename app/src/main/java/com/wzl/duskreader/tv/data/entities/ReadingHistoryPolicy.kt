package com.wzl.duskreader.tv.data.entities

fun Book.hasReadingHistory(): Boolean {
    return lastReadPosition > 0
}

/**
 * 解码 [Book.lastReadPosition] 到 0..1 的阅读进度。
 *
 * 兼容两种存储格式：
 * - 旧版：1..100 表示百分比
 * - 新版：百万分位（由 [com.wzl.duskreader.tv.data.reader.encodeSavedPosition] 写入）
 */
fun Book.progressRatio(): Float = when {
    lastReadPosition <= 0 -> 0f
    lastReadPosition in 1..100 -> lastReadPosition / 100f
    else -> (lastReadPosition / 1_000_000f).coerceIn(0f, 1f)
}
