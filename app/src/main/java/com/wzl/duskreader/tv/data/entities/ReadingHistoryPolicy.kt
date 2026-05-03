package com.wzl.duskreader.tv.data.entities

fun Book.hasReadingHistory(): Boolean {
    return lastReadChapter > 0 || lastReadPosition > 0
}

/**
 * 章节级架构下的粗略阅读进度（0..1）。
 *
 * 计算方式：`lastReadChapter / totalSize`，其中 [Book.totalSize] 在章节级架构中
 * 复用为「总章节数」（由 ReaderViewModel 在首次扫描章节后写入）。
 *
 * 章节内的精细偏移（[Book.lastReadPosition]）在这里不算入，因为它是「章节内字符偏移」，
 * 没有全文统一的分母可换算。
 */
fun Book.progressRatio(): Float {
    val total = totalSize.toInt()
    if (total <= 0) return 0f
    return (lastReadChapter.toFloat() / total).coerceIn(0f, 1f)
}
