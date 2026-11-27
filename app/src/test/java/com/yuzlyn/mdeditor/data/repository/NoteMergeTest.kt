package com.yuzlyn.mdeditor.data.repository

import com.yuzlyn.mdeditor.data.NoteModel
import com.yuzlyn.mdeditor.data.local.NoteDraftEntity
import com.yuzlyn.mdeditor.data.local.NoteEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 守住「掃描資料夾不可以把內容吃掉」的合併規則。
 * 這些情境以前都會讓筆記變成空白：讀檔失敗、權限被回收、寫檔失敗後重開。
 */
class NoteMergeTest {

    private fun scannedNote(
        id: String = NOTE_ID,
        content: String,
        title: String = "檔名標題",
        lastModified: Long = 1_000L,
        contentUnavailable: Boolean = false
    ) = NoteModel(
        id = id,
        title = title,
        uriString = "content://doc/$id",
        content = content,
        notebookTag = "默認筆記本",
        lastModified = lastModified,
        createdAt = lastModified,
        contentUnavailable = contentUnavailable
    )

    private fun savedNote(
        id: String = NOTE_ID,
        title: String = "使用者改的標題",
        content: String,
        lastModified: Long = 1_000L,
        verticalLayout: Boolean? = null,
        backgroundPreset: String? = null,
        backgroundInk: String? = null,
        noteFont: String? = null,
        sourceFormat: String = "",
        sourceUri: String = ""
    ) = NoteEntity(
        id = id,
        title = title,
        uriString = "content://doc/$id",
        content = content,
        notebookTag = "默認筆記本",
        lastModified = lastModified,
        createdAt = 42L,
        verticalLayout = verticalLayout,
        backgroundPreset = backgroundPreset,
        backgroundInk = backgroundInk,
        noteFont = noteFont,
        sourceFormat = sourceFormat,
        sourceUri = sourceUri
    )

    private fun draft(
        id: String = NOTE_ID,
        title: String = "使用者改的標題",
        content: String,
        updatedAt: Long
    ) = NoteDraftEntity(noteId = id, title = title, content = content, updatedAt = updatedAt)

    @Test
    fun `讀檔失敗時保留資料庫內容`() {
        val result = NoteMerge.merge(
            scanned = listOf(
                scannedNote(content = "", lastModified = 9_000L, contentUnavailable = true)
            ),
            saved = mapOf(NOTE_ID to savedNote(content = "寫了很久的內容", lastModified = 1_000L)),
            drafts = emptyMap()
        )

        assertEquals("寫了很久的內容", result.single().content)
    }

    @Test
    fun `全新檔案讀不到時不要建立空筆記`() {
        val result = NoteMerge.merge(
            scanned = listOf(scannedNote(content = "", contentUnavailable = true)),
            saved = emptyMap(),
            drafts = emptyMap()
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `草稿比資料庫新時使用草稿`() {
        val result = NoteMerge.merge(
            scanned = listOf(scannedNote(content = "還沒寫進檔案的舊內容", lastModified = 1_000L)),
            saved = mapOf(NOTE_ID to savedNote(content = "舊的", lastModified = 1_000L)),
            drafts = mapOf(NOTE_ID to draft(content = "剛打的字", updatedAt = 5_000L))
        )

        assertEquals("剛打的字", result.single().content)
        assertEquals(5_000L, result.single().lastModified)
    }

    @Test
    fun `資料庫比草稿新時使用資料庫`() {
        val result = NoteMerge.merge(
            scanned = listOf(scannedNote(content = "舊檔", lastModified = 1_000L)),
            saved = mapOf(NOTE_ID to savedNote(content = "資料庫最新", lastModified = 8_000L)),
            drafts = mapOf(NOTE_ID to draft(content = "比較舊的草稿", updatedAt = 6_000L))
        )

        assertEquals("資料庫最新", result.single().content)
    }

    @Test
    fun `檔案明顯較新時以檔案為準並保留標題與排版`() {
        val result = NoteMerge.merge(
            scanned = listOf(scannedNote(content = "在別的 App 改過", lastModified = 60_000L)),
            saved = mapOf(
                NOTE_ID to savedNote(
                    content = "資料庫內容",
                    lastModified = 1_000L,
                    verticalLayout = true
                )
            ),
            drafts = emptyMap()
        )

        val merged = result.single()
        assertEquals("在別的 App 改過", merged.content)
        assertEquals("使用者改的標題", merged.title)
        assertEquals(true, merged.verticalLayout)
        assertEquals(42L, merged.createdAt)
    }

    @Test
    fun `掃描時保留來源格式與原始檔 Uri`() {
        // 這兩個欄位只有資料庫有（檔名永遠是 .md），兩條合併分支都要接回來，
        // 否則每次啟動、每次重新掃描，卡片徽章和頂欄的 PDF 按鈕都會消失。
        val saved = savedNote(
            content = "資料庫內容",
            lastModified = 60_000L,
            sourceFormat = "pdf",
            sourceUri = "content://doc/source.pdf"
        )

        listOf(1_000L, 90_000L).forEach { fileTime ->
            val result = NoteMerge.merge(
                scanned = listOf(scannedNote(content = "檔案內容 $fileTime", lastModified = fileTime)),
                saved = mapOf(NOTE_ID to saved),
                drafts = emptyMap()
            )

            val merged = result.single()
            assertEquals("pdf", merged.sourceFormat)
            assertEquals("content://doc/source.pdf", merged.sourceUri)
        }
    }

    @Test
    fun `同一秒內的外部修改也算檔案較新`() {
        val result = NoteMerge.merge(
            scanned = listOf(scannedNote(content = "外部修改", lastModified = 10_000L)),
            saved = mapOf(NOTE_ID to savedNote(content = "資料庫內容", lastModified = 10_500L)),
            drafts = emptyMap()
        )

        assertEquals("外部修改", result.single().content)
    }

    @Test
    fun `讀檔失敗但有草稿時使用草稿`() {
        val result = NoteMerge.merge(
            scanned = listOf(
                scannedNote(content = "", lastModified = 9_000L, contentUnavailable = true)
            ),
            saved = emptyMap(),
            drafts = mapOf(NOTE_ID to draft(content = "草稿內容", updatedAt = 3_000L))
        )

        assertEquals("草稿內容", result.single().content)
    }

    @Test
    fun `檔案被外部刪除但還有草稿時保留筆記`() {
        val result = NoteMerge.merge(
            scanned = emptyList(),
            saved = mapOf(NOTE_ID to savedNote(content = "資料庫內容", lastModified = 1_000L)),
            drafts = mapOf(NOTE_ID to draft(content = "草稿內容", updatedAt = 2_000L))
        )

        assertEquals("資料庫內容", result.single().content)
    }

    @Test
    fun `檔案被外部刪除且沒有草稿時移除筆記`() {
        val result = NoteMerge.merge(
            scanned = emptyList(),
            saved = mapOf(NOTE_ID to savedNote(content = "資料庫內容", lastModified = 1_000L)),
            drafts = emptyMap()
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `檔案內容與資料庫一致時沿用資料庫的標題`() {
        val result = NoteMerge.merge(
            scanned = listOf(scannedNote(content = "一樣的內容", lastModified = 1_000L)),
            saved = mapOf(NOTE_ID to savedNote(content = "一樣的內容", lastModified = 2_000L)),
            drafts = emptyMap()
        )

        assertEquals("使用者改的標題", result.single().title)
        assertEquals("一樣的內容", result.single().content)
    }

    /**
     * 單篇樣式（底紙／字色／字體）只存在資料庫裡，掃描時一定要從舊資料接回來。
     * 漏接的話，每次啟動或換資料夾，使用者為某一篇挑的樣式就會悄悄回到全域預設。
     */
    @Test
    fun `掃描不會洗掉單篇的樣式設定`() {
        val result = NoteMerge.merge(
            scanned = listOf(scannedNote(content = "檔案裡的內容", lastModified = 9_000L)),
            saved = mapOf(
                NOTE_ID to savedNote(
                    content = "資料庫內容",
                    lastModified = 1_000L,
                    backgroundPreset = "sepia",
                    backgroundInk = "dark",
                    noteFont = "kai.ttf"
                )
            ),
            drafts = emptyMap()
        )

        assertEquals(1, result.size)
        assertEquals("sepia", result.first().backgroundPreset)
        assertEquals("dark", result.first().backgroundInk)
        assertEquals("kai.ttf", result.first().noteFont)
    }

    private companion object {
        const val NOTE_ID = "content://doc/note-1"
    }
}
