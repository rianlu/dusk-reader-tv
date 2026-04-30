package com.wzl.duskreader.tv.presentation.screens.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderPageTest {

    @Test
    fun buildReaderPages_assignsSequentialOffsets() {
        val pages = buildReaderPages(
            pageTexts = listOf("第一页", "第二页内容"),
            windowStartOffset = 120,
        )

        assertEquals(2, pages.size)
        assertEquals("第一页", pages[0].content)
        assertEquals(120, pages[0].startOffset)
        assertEquals(123, pages[0].endOffset)
        assertEquals("第二页内容", pages[1].content)
        assertEquals(123, pages[1].startOffset)
        assertEquals(128, pages[1].endOffset)
    }

    @Test
    fun remapOffsetByProgress_preservesApproximateRatio() {
        val newOffset = remapOffsetByProgress(
            currentOffset = 300,
            previousLength = 1000,
            newLength = 2000,
        )

        assertEquals(600, newOffset)
    }

    @Test
    fun buildFormattedReaderText_usesCompactSpacingByDefault() {
        val text = buildFormattedReaderText(
            paragraphs = listOf("第一段", "第二段"),
            paragraphSpacing = 16,
        )

        assertEquals("　　第一段\n\n　　第二段", text)
    }

    @Test
    fun buildFormattedReaderText_expandsSeparatorForLargerSpacing() {
        val text = buildFormattedReaderText(
            paragraphs = listOf("第一段", "第二段"),
            paragraphSpacing = 28,
        )

        assertEquals("　　第一段\n\n\n\n　　第二段", text)
    }

    @Test
    fun progressRatioForOffset_clampsToValidRange() {
        assertEquals(0f, progressRatioForOffset(currentOffset = 10, fullTextLength = 0))
        assertTrue(progressRatioForOffset(currentOffset = 999, fullTextLength = 100) <= 1f)
    }
}
