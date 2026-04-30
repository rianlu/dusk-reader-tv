package com.wzl.duskreader.tv.presentation.screens.reader

data class ReaderPage(
    val content: String,
    val startOffset: Int,
    val endOffset: Int,
) {
    val consumedLength: Int
        get() = (endOffset - startOffset).coerceAtLeast(0)
}

internal fun buildReaderPages(
    pageTexts: List<String>,
    windowStartOffset: Int = 0,
): List<ReaderPage> {
    if (pageTexts.isEmpty()) return emptyList()

    var currentOffset = windowStartOffset
    return pageTexts.map { pageText ->
        ReaderPage(
            content = pageText,
            startOffset = currentOffset,
            endOffset = currentOffset + pageText.length,
        ).also {
            currentOffset = it.endOffset
        }
    }
}
