package com.yuzlyn.mdeditor.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 「自動帶入畫面」規格的迴歸測試。
 *
 * 這裡鎖的是三條規則：手勢中不動、比視窗高的請求不動、位移過大的請求不動。
 * 三者任何一條失守，長按選字時畫面就會被系統拉走。
 */
class EditorBringIntoViewSpecTest {

    private val viewport = 1000f

    private fun EditorBringIntoViewSpec.distance(offset: Float, size: Float) =
        calculateScrollDistance(offset, size, viewport)

    @Test
    fun `手勢進行中一律不動`() {
        val spec = EditorBringIntoViewSpec()
        spec.gestureInProgress = true

        // 游標在視窗上方 200px，正常會往回捲；手勢中就該維持原樣。
        assertEquals(0f, spec.distance(offset = -200f, size = 40f), 0.001f)
        spec.gestureInProgress = false
        assertEquals(-200f, spec.distance(offset = -200f, size = 40f), 0.001f)
    }

    @Test
    fun `整塊輸入框比視窗高時不動`() {
        val spec = EditorBringIntoViewSpec()

        // 輸入框高度等於整篇正文，遠比視窗高：對齊任何一端都是大跳。
        assertEquals(0f, spec.distance(offset = -3000f, size = 9000f), 0.001f)
        assertEquals(0f, spec.distance(offset = 0f, size = 1000f), 0.001f)
    }

    @Test
    fun `游標完全在視窗內時不動`() {
        val spec = EditorBringIntoViewSpec()

        assertEquals(0f, spec.distance(offset = 400f, size = 40f), 0.001f)
    }

    @Test
    fun `游標在視窗上方時往回捲最小距離`() {
        val spec = EditorBringIntoViewSpec()

        assertEquals(-24f, spec.distance(offset = -24f, size = 40f), 0.001f)
    }

    @Test
    fun `游標在視窗下方時往後捲最小距離`() {
        val spec = EditorBringIntoViewSpec()

        // offset 990 + size 40 = 1030，超出視窗 30。
        assertEquals(30f, spec.distance(offset = 990f, size = 40f), 0.001f)
    }

    @Test
    fun `位移超過一個視窗時視為錯誤座標`() {
        val spec = EditorBringIntoViewSpec()

        // 長按時焦點事件帶來的舊游標可能停在文件開頭，這條請求若被執行就是「跳回最上面」。
        assertEquals(0f, spec.distance(offset = -1200f, size = 40f), 0.001f)
    }

    @Test
    fun `視窗尺寸未知時不動`() {
        val spec = EditorBringIntoViewSpec()

        assertEquals(0f, spec.calculateScrollDistance(offset = -40f, size = 20f, containerSize = 0f), 0.001f)
    }
}
