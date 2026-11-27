package com.yuzlyn.mdeditor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = NoteEntity.TABLE_NAME)
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val uriString: String,
    val content: String,
    val notebookTag: String,
    val lastModified: Long,
    val createdAt: Long,
    /** 這篇筆記自己的排版；null 表示沿用設定裡的預設排版。 */
    val verticalLayout: Boolean? = null,
    /** 這篇筆記自己的書寫輔助線（WritingGuide.id）；null 表示沿用設定裡的預設。 */
    val writingGuide: String? = null,
    /** 這篇筆記自己的底紙（NoteBackground.id）；null 表示沿用設定裡的預設。 */
    val backgroundPreset: String? = null,
    /** 這篇筆記自己的字色（NoteBackgroundInk.id）；null 表示沿用設定裡的預設。 */
    val backgroundInk: String? = null,
    /** 這篇筆記自己的字體（FontStore 的檔名或 SYSTEM）；null 表示沿用設定裡的預設。 */
    val noteFont: String? = null,
    /** 來源檔案類型（ImportFormat.id）；空字串＝App 內新建。 */
    val sourceFormat: String = "",
    /** 來源檔案 Uri；空字串＝沒有可回open 的原始檔（或權限沒拿到）。 */
    val sourceUri: String = ""
) {
    companion object {
        const val TABLE_NAME = "mdeditor_notes"
    }
}
