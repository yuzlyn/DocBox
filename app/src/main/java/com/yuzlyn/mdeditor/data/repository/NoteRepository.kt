package com.yuzlyn.mdeditor.data.repository

import androidx.room.withTransaction
import com.yuzlyn.mdeditor.data.NoteModel
import com.yuzlyn.mdeditor.data.local.AppDatabase
import com.yuzlyn.mdeditor.data.local.NoteDraftEntity
import com.yuzlyn.mdeditor.data.local.toDomain
import com.yuzlyn.mdeditor.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class NoteRepository(private val database: AppDatabase) {

    private val dao = database.noteDao()
    private val draftDao = database.noteDraftDao()

    val notes: Flow<List<NoteModel>> = dao.observeNotes().map { entities ->
        entities.map { it.toDomain() }
    }

    fun observeNote(noteId: String): Flow<NoteModel?> =
        dao.observeNote(noteId).map { it?.toDomain() }

    suspend fun getNote(noteId: String): NoteModel? = dao.getNote(noteId)?.toDomain()

    /**
     * 把資料夾掃描結果併回資料庫。
     *
     * 舊版是「先清空再整批寫入」，只要掃描當下有任何一篇讀不到（權限被回收、
     * 雲端硬碟還沒同步、卡片掛載失敗），資料庫裡那份內容就會被空字串蓋掉，
     * 而且完全沒有痕跡。現在改成逐篇判斷「誰比較新」：
     *
     * 1. 草稿與資料庫取時間較新的那一份（草稿較新＝檔案還沒寫成功；資料庫較新＝
     *    剛寫完資料庫就斷電）。
     * 2. 這次讀檔失敗：一律保留手上的內容，不要拿空字串蓋。
     * 3. 檔案明顯比手上內容新：使用者在別的 App 改過，以檔案為準，
     *    但標題、排版、建立時間仍沿用資料庫。
     */
    suspend fun applyScan(scanned: List<NoteModel>) {
        database.withTransaction {
            val merged = NoteMerge.merge(
                scanned = scanned,
                saved = dao.getAll().associateBy { it.id },
                drafts = draftDao.getAll().associateBy { it.noteId }
            )
            dao.deleteAll()
            dao.insertAll(merged)
        }
    }

    /** 設定單篇筆記的排版；null 表示改回沿用預設。 */
    suspend fun setVerticalLayout(noteId: String, vertical: Boolean?) =
        dao.updateVerticalLayout(noteId, vertical?.let { if (it) 1 else 0 })

    /** 單篇的書寫輔助線；null 表示恢復成跟隨預設。 */
    suspend fun setWritingGuide(noteId: String, guide: String?) =
        dao.updateWritingGuide(noteId, guide)

    suspend fun insert(note: NoteModel) = dao.insert(note.toEntity())

    suspend fun update(noteId: String, title: String, content: String, lastModified: Long) =
        dao.update(noteId, title, content, lastModified)

    suspend fun delete(noteId: String) = dao.delete(noteId)

    // ---- 自動保存草稿 ----------------------------------------------------

    suspend fun writeDraft(noteId: String, title: String, content: String) {
        draftDao.upsert(
            NoteDraftEntity(
                noteId = noteId,
                title = title,
                content = content,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun getDraft(noteId: String): NoteDraftEntity? = draftDao.get(noteId)

    suspend fun allDrafts(): List<NoteDraftEntity> = draftDao.getAll()

    suspend fun deleteDraft(noteId: String) = draftDao.delete(noteId)

    suspend fun pruneDrafts(cutoff: Long) = draftDao.deleteOlderThan(cutoff)

    /** 記下這篇筆記的原始檔（目前用來回開 PDF）。 */
    suspend fun setSourceDocument(noteId: String, format: String, uri: String) =
        dao.setSourceDocument(noteId, format, uri)

    /**
     * 啟動自檢：把已經讀不到的來源 Uri 清掉。
     * 檔案被刪、被移走、或使用者在系統設定裡撤銷授權時，
     * 頂欄那顆「開啟原始檔」就會變成一顆按了必然失敗的按鈕——不如不顯示。
     */
    suspend fun pruneSourceUris(valid: Set<String>) {
        if (valid.isEmpty()) dao.clearAllSourceUris() else dao.clearSourceUrisNotIn(valid.toList())
    }
}
