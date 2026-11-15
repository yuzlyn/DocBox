package com.yuzlyn.mdeditor.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * 插入圖片時「檔名／MIME／替代文字」的推導規則。
 * 這些值會直接寫進使用者的 Markdown，錯了就是一個壞掉的連結。
 */
class ImageAttachmentTest {

    @Test
    fun `mime 由副檔名推回且不分大小寫`() {
        assertEquals("image/jpeg", imageMimeForName("IMG_0001.JPG"))
        assertEquals("image/jpeg", imageMimeForName("photo.jpeg"))
        assertEquals("image/png", imageMimeForName("shot.png"))
        assertEquals("image/webp", imageMimeForName("a.webp"))
        assertEquals("image/svg+xml", imageMimeForName("icon.svg"))
        assertEquals("image/heic", imageMimeForName("IMG_0001.HEIC"))
    }

    @Test
    fun `認不得的副檔名回 null`() {
        assertNull(imageMimeForName("notes.txt"))
        assertNull(imageMimeForName("no-extension"))
    }

    @Test
    fun `副檔名由 mime 決定`() {
        assertEquals("jpg", imageExtensionFor("image/jpeg", "whatever"))
        assertEquals("png", imageExtensionFor("image/png", "whatever"))
        assertEquals("webp", imageExtensionFor("image/webp", "whatever"))
        assertEquals("gif", imageExtensionFor("image/gif", "whatever"))
    }

    @Test
    fun `mime 不明時借用原檔名`() {
        assertEquals("heic", imageExtensionFor("application/octet-stream", "IMG_0001.heic"))
        assertEquals("png", imageExtensionFor("", "shot.PNG"))
    }

    @Test
    fun `都認不得時退回 jpg`() {
        assertEquals("jpg", imageExtensionFor("application/octet-stream", "no-extension"))
        assertEquals("jpg", imageExtensionFor("", ""))
    }

    @Test
    fun `替代文字清掉會破壞連結語法的字元`() {
        assertEquals("a_b", sanitizeImageAlt("a[b"))
        assertEquals("a_b_c", sanitizeImageAlt("a]b)c"))
        assertEquals("shot 2024", sanitizeImageAlt("shot 2024"))
    }

    @Test
    fun `空替代文字退回 image`() {
        assertEquals("image", sanitizeImageAlt(""))
        assertEquals("image", sanitizeImageAlt("   "))
    }
}
