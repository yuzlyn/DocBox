package com.yuzlyn.mdeditor

import com.yuzlyn.mdeditor.data.book.ReaderMode
import com.yuzlyn.mdeditor.data.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 翻頁方式的三種取值。
 *
 * 這組值會寫進 SharedPreferences，改動等於改存檔格式，所以把「舊值怎麼換算」
 * 也一併釘住：以前只有 `scroll` 一個值，軸向跟著排版方向走。
 */
class ReaderModeTest {

    @Test
    fun `id 可以還原成同一個值`() {
        ReaderMode.entries.forEach { mode ->
            assertEquals(mode, ReaderMode.fromId(mode.id))
        }
    }

    @Test
    fun `認不得的字串退回翻頁`() {
        assertEquals(ReaderMode.PAGED, ReaderMode.fromId(null))
        assertEquals(ReaderMode.PAGED, ReaderMode.fromId(""))
        assertEquals(ReaderMode.PAGED, ReaderMode.fromId("something-else"))
        // 舊版的單一滾動值不在新枚舉裡，但它仍然要算「認得」，否則存檔會被洗成翻頁。
        assertEquals(ReaderMode.PAGED, ReaderMode.fromId("scroll"))
        assertTrue(ReaderMode.isKnown("scroll"))
    }

    @Test
    fun `預設方向跟著排版走`() {
        assertEquals(ReaderMode.SCROLL_VERTICAL, ReaderMode.defaultFor(vertical = false))
        assertEquals(ReaderMode.SCROLL_HORIZONTAL, ReaderMode.defaultFor(vertical = true))
    }

    @Test
    fun `滾動軸由模式決定 不看排版方向`() {
        assertFalse(ReaderMode.PAGED.isScroll)
        assertTrue(ReaderMode.SCROLL_VERTICAL.isScroll)
        assertTrue(ReaderMode.SCROLL_HORIZONTAL.isScroll)
        assertFalse(ReaderMode.SCROLL_VERTICAL.scrollsHorizontally)
        assertTrue(ReaderMode.SCROLL_HORIZONTAL.scrollsHorizontally)
    }

    @Test
    fun `舊存檔 scroll 依排版方向換算`() {
        assertEquals(
            ReaderMode.SCROLL_VERTICAL,
            AppSettings(readerMode = "scroll", readerVertical = false).readingMode
        )
        assertEquals(
            ReaderMode.SCROLL_HORIZONTAL,
            AppSettings(readerMode = "scroll", readerVertical = true).readingMode
        )
    }

    @Test
    fun `新存檔照原樣還原`() {
        assertEquals(
            ReaderMode.SCROLL_HORIZONTAL,
            AppSettings(readerMode = "scroll-h", readerVertical = false).readingMode
        )
        assertEquals(
            ReaderMode.PAGED,
            AppSettings(readerMode = "paged", readerVertical = true).readingMode
        )
    }
}
