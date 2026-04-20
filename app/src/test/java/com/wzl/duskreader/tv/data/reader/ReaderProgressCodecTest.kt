package com.wzl.duskreader.tv.data.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class ReaderProgressCodecTest {
    @Test
    fun decodeSavedPosition_supportsLegacyPercentEncoding() {
        assertEquals(500, decodeSavedPosition(savedPos = 50, fullTextLength = 1000))
    }

    @Test
    fun decodeSavedPosition_supportsMillionScaleEncoding() {
        assertEquals(250, decodeSavedPosition(savedPos = 250000, fullTextLength = 1000))
    }

    @Test
    fun encodeSavedPosition_returnsMillionScaleProgress() {
        assertEquals(250000, encodeSavedPosition(currentOffset = 250, fullTextLength = 1000))
    }
}
