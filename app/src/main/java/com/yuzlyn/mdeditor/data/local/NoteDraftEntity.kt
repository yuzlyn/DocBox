package com.yuzlyn.mdeditor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 編輯中、還沒成功寫回 `.md` 檔的草稿。
 *
 * 只要檔案還沒落盤，這筆資料就留著；下次啟動時 [com.yuzlyn.mdeditor.data.NoteAutoSaver]
 * 會把它寫回檔案並刪除。用途有三個：
 * 1. 打字打到一半被系統砍掉（強制停止、OOM、當機）→ 重新打開還在。
 * 2. SAF 寫入失敗（權限、雲端離線）→ 內容不會只剩在記憶體裡。
 * 3. 寫到一半斷電被截斷 → 草稿會再寫一次，蓋掉壞掉的檔案。
 */
@Entity(tableName = NoteDraftEntity.TABLE_NAME)
data class NoteDraftEntity(
    @PrimaryKey val noteId: String,
    val title: String,
    val content: String,
    val updatedAt: Long
) {
    companion object {
        const val TABLE_NAME = "mdeditor_note_drafts"
    }
}
