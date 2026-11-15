package com.yuzlyn.mdeditor.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 游標滾動策略的迴歸測試。
 *
 * 這裡鎖住三件事：
 * 1. 游標在可視範圍內 → 位移必須是 0（不置中、不回正）。
 * 2. 被上／下邊界遮住 → 只補剛好露出的最小距離。
 * 3. 文件捲得再深，只要游標在視口內就不該產生位移（舊版的跳動根因）。
 */
class CaretScrollTest {

    @Test
    fun `游標在安全區內完全不動`() {
        assertEquals(0f, caretScrollDelta(500f, 540f, safeTop = 8f, safeBottom = 1000f), 0.001f)
    }

    @Test
    fun `游標被底欄遮住時只補最小距離`() {
        // 底欄上緣 1000、游標下緣 1030：只把內容往上推 30。
        assertEquals(30f, caretScrollDelta(990f, 1030f, safeTop = 8f, safeBottom = 1000f), 0.001f)
    }

    @Test
    fun `游標被頂部遮住時只補最小距離`() {
        assertEquals(-12f, caretScrollDelta(-4f, 36f, safeTop = 8f, safeBottom = 1000f), 0.001f)
    }

    @Test
    fun `剛好貼在邊界上不校正`() {
        assertEquals(0f, caretScrollDelta(8f, 1000f, safeTop = 8f, safeBottom = 1000f), 0.001f)
    }

    @Test
    fun `游標比安全區還高時以下緣為準`() {
        // 上下都被遮住：先讓下緣露出來，避免無限往上推。
        assertEquals(40f, caretScrollDelta(-20f, 1040f, safeTop = 8f, safeBottom = 1000f), 0.001f)
    }

    @Test
    fun `安全區無效時不校正`() {
        assertEquals(0f, caretScrollDelta(500f, 540f, safeTop = 1000f, safeBottom = 900f), 0.001f)
        assertEquals(0f, caretScrollDelta(500f, 540f, safeTop = 1000f, safeBottom = 1000f), 0.001f)
    }

    @Test
    fun `捲到文件深處後游標在視口內不滾動`() {
        // 內容座標 4200 的游標、目前滾到 4000 → 視口座標 200，屬安全區。
        // 舊版拿內容座標和滾動值比較，會算出「捲到 8200」這種位移，就是畫面亂跳的來源。
        val scroll = 4000f
        assertEquals(
            0f,
            caretScrollDelta(
                4200f - scroll,
                4240f - scroll,
                safeTop = 8f,
                safeBottom = 1000f
            ),
            0.001f
        )
    }

    @Test
    fun `捲到文件深處後游標被底欄遮住仍只補最小距離`() {
        val scroll = 4000f
        // 游標下緣在視口 1030，超出底欄上緣 1000 → 補 30。
        assertEquals(
            30f,
            caretScrollDelta(
                4990f - scroll,
                5030f - scroll,
                safeTop = 8f,
                safeBottom = 1000f
            ),
            0.001f
        )
    }

    @Test
    fun `滾動目標會被夾在文件範圍內`() {
        assertEquals(0f, clampScrollTarget(10f, -100f, max = 5000f), 0.001f)
        assertEquals(5000f, clampScrollTarget(4990f, 100f, max = 5000f), 0.001f)
        assertEquals(120f, clampScrollTarget(100f, 20f, max = 5000f), 0.001f)
    }

    @Test
    fun `沒有可滾動範圍時固定回零`() {
        assertEquals(0f, clampScrollTarget(0f, 50f, max = 0f), 0.001f)
    }
}
