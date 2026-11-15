package com.yuzlyn.mdeditor.ui.reader

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.yuzlyn.mdeditor.data.book.ReadingColors
import com.yuzlyn.mdeditor.data.book.ReadingTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 閱讀主題的莫奈派生。
 *
 * 這裡顧的是「換了種子色之後，每一套紙還能不能讀」——配色是算出來的，
 * 不是校過的常數，所以每次動派生規則都要重跑一次對比度。
 */
class ReadingPaletteTest {

    /** 幾個色相差很遠的種子：紫（M3 預設）、藍、橙、綠、幾乎無彩。 */
    private val seeds = listOf(
        "紫" to Color(0xFF6750A4),
        "藍" to Color(0xFF1F6FEB),
        "橙" to Color(0xFFFF7A00),
        "綠" to Color(0xFF2E7D32),
        "灰" to Color(0xFF6B6B6B)
    )

    private fun palettes(seed: Color, dark: Boolean = false) = readingPalettes(
        seed = seed,
        appDark = dark,
        appSurface = if (dark) Color(0xFF141218) else Color(0xFFFFF7FF),
        appOnSurface = if (dark) Color(0xFFE6E0E9) else Color(0xFF1D1B20),
        appPrimary = seed
    )

    @Test
    fun `每套主題正文都過 WCAG AA`() {
        for ((name, seed) in seeds) {
            val palettes = palettes(seed)
            assertEquals(ReadingTheme.entries.size, palettes.size)
            for (theme in ReadingTheme.entries) {
                val ratio = contrast(palettes.getValue(theme))
                assertTrue(
                    "$name／${theme.id} 對比只有 ${"%.2f".format(ratio)}",
                    ratio >= 4.5
                )
            }
        }
    }

    @Test
    fun `預設就是 App 自己的配色`() {
        val colors = palettes(Color(0xFF6750A4)).getValue(ReadingTheme.DEFAULT)
        assertEquals(0xFFF7FFL, colors.background)
        assertEquals(0x1D1B20L, colors.foreground)
        assertEquals(0x6750A4L, colors.link)
        assertTrue(!colors.dark)
    }

    @Test
    fun `深色 App 的預設跟著轉深`() {
        val colors = palettes(Color(0xFF6750A4), dark = true).getValue(ReadingTheme.DEFAULT)
        assertTrue(colors.dark)
        assertTrue(luminanceOf(colors.background) < 0.1)
    }

    @Test
    fun `素白與墨黑是無彩度的 其餘跟著種子色換`() {
        val purple = palettes(Color(0xFF6750A4))
        val orange = palettes(Color(0xFFFF7A00))

        // 素白與墨黑刻意不吃種子色：一張白紙、一張黑紙，換壁紙也該還是白的/黑的。
        for (theme in listOf(ReadingTheme.PAPER, ReadingTheme.INK)) {
            val paper = purple.getValue(theme).background
            assertEquals(
                "${theme.id} 的底紙不該隨種子色變",
                paper,
                orange.getValue(theme).background
            )
            assertEquals("${theme.id} 的底紙應該是純灰階", 0L, chroma(paper))
        }

        for (theme in listOf(ReadingTheme.CREAM, ReadingTheme.NIGHT)) {
            assertTrue(
                "${theme.id} 換了種子色卻沒變色",
                purple.getValue(theme) != orange.getValue(theme)
            )
        }
    }

    @Test
    fun `深淺是一條看得出來的階梯`() {
        val palettes = palettes(Color(0xFF6750A4))
        val paper = luminanceOf(palettes.getValue(ReadingTheme.PAPER).background)
        val cream = luminanceOf(palettes.getValue(ReadingTheme.CREAM).background)
        val night = luminanceOf(palettes.getValue(ReadingTheme.NIGHT).background)
        val ink = luminanceOf(palettes.getValue(ReadingTheme.INK).background)

        assertTrue("素白應該接近純白", paper > 0.9)
        assertTrue("素白要亮於淡彩", paper > cream)
        assertTrue("淡彩要亮於夜色", cream > night)
        assertTrue("夜色要亮於墨黑", night > ink)
        // 兩兩之間有差，圓點排在一起才認得出是不同主題。
        assertTrue("素白與淡彩差太少", paper - cream > 0.02)
        assertTrue("夜色與墨黑差太少", night - ink > 0.003)
    }

    private fun contrast(colors: ReadingColors): Double {
        val a = luminanceOf(colors.background) + 0.05
        val b = luminanceOf(colors.foreground) + 0.05
        return if (a > b) a / b else b / a
    }

    private fun luminanceOf(rgb: Long): Double = Color(0xFF000000L or rgb).luminance().toDouble()

    /** 最高與最低通道的差；0 就是純灰階。 */
    private fun chroma(rgb: Long): Long {
        val r = (rgb shr 16) and 0xFF
        val g = (rgb shr 8) and 0xFF
        val b = rgb and 0xFF
        return maxOf(r, g, b) - minOf(r, g, b)
    }
}
