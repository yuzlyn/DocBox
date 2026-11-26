package com.yuzlyn.mdeditor.ui.component

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 瀑布流欄數的守門測試。
 *
 * 這些數字不是隨便挑的：上一版用 `StaggeredGridCells.Adaptive(180.dp)`，
 * 360dp 手機扣掉左右 12dp 內邊距後只剩 336dp，只算出一列，
 * 瀑布流退化成單列、和列表長得一模一樣，排版切換看起來就完全失效。
 * 這裡把「手機必須兩列」釘死，避免有人再把基準改大。
 */
class MasonryColumnsTest {

    @Test
    fun phone360KeepsTwoColumns() {
        // 360dp 視窗 - 左右各 12dp。
        assertEquals(2, masonryColumns(336.dp))
    }

    @Test
    fun narrowSplitScreenFallsBackToSingleColumn() {
        // 分屏只剩 280dp 時硬塞兩欄，標題會被截到看不懂。
        assertEquals(1, masonryColumns(280.dp))
    }

    @Test
    fun widerScreensAddColumns() {
        assertEquals(2, masonryColumns(411.dp - 24.dp))
        assertEquals(3, masonryColumns(600.dp - 24.dp))
        assertEquals(4, masonryColumns(840.dp - 24.dp))
    }

    @Test
    fun columnCountIsCapped() {
        assertEquals(6, masonryColumns(1600.dp))
    }
}
