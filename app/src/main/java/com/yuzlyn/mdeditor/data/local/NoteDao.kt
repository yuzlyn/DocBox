package com.yuzlyn.mdeditor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM ${NoteEntity.TABLE_NAME} ORDER BY lastModified DESC")
    fun observeNotes(): Flow<List<NoteEntity>>

    @Query("SELECT * FROM ${NoteEntity.TABLE_NAME} WHERE id = :noteId")
    fun observeNote(noteId: String): Flow<NoteEntity?>

    @Query("SELECT * FROM ${NoteEntity.TABLE_NAME} WHERE id = :noteId")
    suspend fun getNote(noteId: String): NoteEntity?

    @Query("SELECT * FROM ${NoteEntity.TABLE_NAME}")
    suspend fun getAll(): List<NoteEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity)

    @Query(
        "UPDATE ${NoteEntity.TABLE_NAME} " +
                "SET title = :title, content = :content, lastModified = :lastModified " +
                "WHERE id = :noteId AND (title != :title OR content != :content)"
    )
    suspend fun update(noteId: String, title: String, content: String, lastModified: Long)

    /** 只改這一篇筆記的排版；value 為 null 表示改回「沿用預設」。 */
    @Query(
        "UPDATE ${NoteEntity.TABLE_NAME} " +
                "SET verticalLayout = :value WHERE id = :noteId"
    )
    suspend fun updateVerticalLayout(noteId: String, value: Int?)

    /** 單篇的書寫輔助線；傳 null 表示恢復成「跟隨預設」。 */
    @Query(
        "UPDATE ${NoteEntity.TABLE_NAME} " +
                "SET writingGuide = :value WHERE id = :noteId"
    )
    suspend fun updateWritingGuide(noteId: String, value: String?)
    /** 單篇的底紙；傳 null 表示恢復成「跟隨預設」。 */
    @Query(
        "UPDATE ${NoteEntity.TABLE_NAME} " +
                "SET backgroundPreset = :value WHERE id = :noteId"
    )
    suspend fun updateBackgroundPreset(noteId: String, value: String?)

    /** 單篇的字色（背景圖片上的文字顏色）；傳 null 表示跟隨預設。 */
    @Query(
        "UPDATE ${NoteEntity.TABLE_NAME} " +
                "SET backgroundInk = :value WHERE id = :noteId"
    )
    suspend fun updateBackgroundInk(noteId: String, value: String?)

    /** 單篇的字體；傳 null 表示跟隨預設。 */
    @Query(
        "UPDATE ${NoteEntity.TABLE_NAME} " +
                "SET noteFont = :value WHERE id = :noteId"
    )
    suspend fun updateNoteFont(noteId: String, value: String?)

    /** 關聯（或更換）原始檔；同時把來源格式寫成 :format，卡片徽章才對得上。 */
    @Query(
        "UPDATE ${NoteEntity.TABLE_NAME} " +
                "SET sourceFormat = :format, sourceUri = :uri WHERE id = :noteId"
    )
    suspend fun setSourceDocument(noteId: String, format: String, uri: String)

    /** 啟動自檢用：清掉所有來源 Uri（目前沒有任何常駐權限）。 */
    @Query("UPDATE ${NoteEntity.TABLE_NAME} SET sourceUri = '' WHERE sourceUri != ''")
    suspend fun clearAllSourceUris()

    /** 啟動自檢用：只保留還能讀的 Uri，其餘清空。 */
    @Query(
        "UPDATE ${NoteEntity.TABLE_NAME} " +
                "SET sourceUri = '' WHERE sourceUri != '' AND sourceUri NOT IN (:validUris)"
    )
    suspend fun clearSourceUrisNotIn(validUris: List<String>)

    @Query("DELETE FROM ${NoteEntity.TABLE_NAME} WHERE id = :noteId")
    suspend fun delete(noteId: String)

    @Query("DELETE FROM ${NoteEntity.TABLE_NAME}")
    suspend fun deleteAll()
}
