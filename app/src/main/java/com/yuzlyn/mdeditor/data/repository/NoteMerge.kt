package com.yuzlyn.mdeditor.data.repository

import com.yuzlyn.mdeditor.data.NoteModel
import com.yuzlyn.mdeditor.data.local.NoteDraftEntity
import com.yuzlyn.mdeditor.data.local.NoteEntity
import com.yuzlyn.mdeditor.data.local.toEntity

/**
 * 資料夾掃描結果與資料庫的合併規則。
 *
 * 這裡是「內容會不會消失」的最後一道關卡，所以抽成不碰 IO 的純函式，
 * 由單元測試一條一條守住：
 *
 * 1. 全新檔案：讀得到才納入，讀不到就等下次掃描。
 * 2. 草稿與資料庫：取時間較新的那一份。
 * 3. 這次讀檔失敗：一律保留手上的內容，不能拿空字串蓋。
 * 4. 檔案明顯較新：視為在別的 App 改過，以檔案為準，但標題／排版／建立時間沿用資料庫。
 * 5. 檔案不見了：沒有草稿才移除，有草稿就留下來，免得內容跟著檔案一起消失。
 */
internal object NoteMerge {

    /** 檔案 mtime 多半只有秒級精度，比對時放寬一秒。 */
    const val MTIME_GRACE_MS = 1_000L

    fun merge(
        scanned: List<NoteModel>,
        saved: Map<String, NoteEntity>,
        drafts: Map<String, NoteDraftEntity>
    ): List<NoteEntity> {
        val merged = mutableListOf<NoteEntity>()
        val scannedIds = scanned.mapTo(mutableSetOf()) { it.id }

        scanned.forEach { note ->
            val previous = saved[note.id]
            val draft = drafts[note.id]

            // 全新檔案：讀不到就別建一筆空筆記，下次掃描再說。
            if (previous == null && draft == null) {
                if (!note.contentUnavailable) merged.add(note.toEntity())
                return@forEach
            }

            val draftIsNewer = draft != null && draft.updatedAt >= (previous?.lastModified ?: 0L)
            val baseTitle = if (draftIsNewer) {
                draft?.title ?: note.title
            } else {
                previous?.title ?: note.title
            }
            val baseContent = if (draftIsNewer) {
                draft?.content ?: note.content
            } else {
                previous?.content ?: note.content
            }
            val baseUpdatedAt = if (draftIsNewer) {
                draft?.updatedAt ?: note.lastModified
            } else {
                previous?.lastModified ?: note.lastModified
            }

            val fileIsNewer = !note.contentUnavailable &&
                    note.content != baseContent &&
                    note.lastModified > baseUpdatedAt - MTIME_GRACE_MS

            // 來源格式只存在資料庫裡（檔名永遠是 .md），掃描時要從舊資料接回來，
            // 否則每次啟動、每次換資料夾，卡片上的 Word/PDF 徽章都會掉。
            val entity = if (fileIsNewer) {
                note.copy(
                    title = baseTitle,
                    createdAt = previous?.createdAt ?: note.createdAt,
                    verticalLayout = previous?.verticalLayout,
                    writingGuide = previous?.writingGuide,
                    sourceFormat = previous?.sourceFormat ?: "",
                    sourceUri = previous?.sourceUri ?: ""
                )
            } else {
                note.copy(
                    title = baseTitle,
                    content = baseContent,
                    lastModified = baseUpdatedAt,
                    createdAt = previous?.createdAt ?: note.createdAt,
                    verticalLayout = previous?.verticalLayout,
                    writingGuide = previous?.writingGuide,
                    sourceFormat = previous?.sourceFormat ?: "",
                    sourceUri = previous?.sourceUri ?: ""
                )
            }
            merged.add(entity.toEntity())
        }

        // 資料庫有、這次掃描沒看到：檔案被外部刪掉就一起移除，
        // 但還有草稿的留下來，免得內容跟著檔案一起消失。
        saved.values.forEach { entity ->
            if (entity.id !in scannedIds && drafts.containsKey(entity.id)) {
                merged.add(entity)
            }
        }

        return merged
    }
}
