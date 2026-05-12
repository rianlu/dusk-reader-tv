package com.wzl.duskreader.tv.presentation.screens.reader

import org.junit.Assert.assertEquals
import org.junit.Test

class PageTurnModeTest {

    @Test
    fun readerTheme_usesTvFriendlyOrderAndLabels() {
        val labels = ReaderTheme.values().map { it.displayName }
        assertEquals(listOf("墨绿夜读", "影院暗灰", "暖纸柔光", "高对比"), labels)
    }

    @Test
    fun readerTextBrightness_hasThreeSimpleLevels() {
        val labels = ReaderTextBrightness.values().map { it.displayName }
        assertEquals(listOf("柔和", "标准", "清晰"), labels)
    }

    @Test
    fun pageTurnMode_hasThreeOptionsWithChineseLabels() {
        val labels = PageTurnMode.values().map { it.displayName }
        assertEquals(listOf("左右键", "上下键", "自动翻页"), labels)
    }

    @Test
    fun autoTurnInterval_defaultIsTenSeconds() {
        assertEquals(10, AutoTurnInterval.DEFAULT_SECONDS)
    }

    @Test
    fun autoTurnInterval_decrementClampsAtMin() {
        assertEquals(5, AutoTurnInterval.decrement(10))
        assertEquals(5, AutoTurnInterval.decrement(5))
    }

    @Test
    fun autoTurnInterval_incrementClampsAtMax() {
        assertEquals(15, AutoTurnInterval.increment(10))
        assertEquals(60, AutoTurnInterval.increment(60))
    }

    @Test
    fun autoTurnInterval_stepsBy5SecondsAcrossRange() {
        var value = AutoTurnInterval.MIN_SECONDS
        val sequence = mutableListOf(value)
        repeat((AutoTurnInterval.MAX_SECONDS - AutoTurnInterval.MIN_SECONDS) / AutoTurnInterval.STEP_SECONDS) {
            value = AutoTurnInterval.increment(value)
            sequence += value
        }
        assertEquals(listOf(5, 10, 15, 20, 25, 30, 35, 40, 45, 50, 55, 60), sequence)
    }

    @Test
    fun autoTurnInterval_unalignedValueSnapsToStep() {
        // 防御异常输入：12 ↑ 应跳到 15、12 ↓ 应跳到 10
        assertEquals(15, AutoTurnInterval.increment(12))
        assertEquals(10, AutoTurnInterval.decrement(12))
    }
}
