package com.yuzlyn.mdeditor.data

data class NoteModel(
    val id: String,
    val title: String,
    val uriString: String,
    val content: String,
    val notebookTag: String,
    val lastModified: Long,
    val createdAt: Long,
    /** 這篇筆記自己的排版（true 直排 / false 橫排）；null 表示沿用設定裡的預設排版。 */
    val verticalLayout: Boolean? = null,
    /** 這篇筆記自己的書寫輔助線（WritingGuide.id）；null 表示沿用設定裡的預設。 */
    val writingGuide: String? = null,
    /** 這篇筆記自己的底紙（NoteBackground.id）；null 表示沿用設定裡的預設。 */
    val backgroundPreset: String? = null,
    /** 這篇筆記自己的字色（NoteBackgroundInk.id）；null 表示沿用設定裡的預設。 */
    val backgroundInk: String? = null,
    /** 這篇筆記自己的字體（FontStore 的檔名或 SYSTEM）；null 表示沿用設定裡的預設。 */
    val noteFont: String? = null,
    /**
     * 只在資料夾掃描時使用：true 表示這次讀檔失敗（不是檔案本來就空），
     * 匯入時要保留資料庫裡既有的內容，不能拿空字串蓋掉。
     */
    val contentUnavailable: Boolean = false,
    /**
     * 這篇筆記的來源檔案類型（Word／PDF／EPUB…），存的是 [com.yuzlyn.mdeditor.data.importer.ImportFormat.id]。
     * 空字串＝在 App 裡新建的筆記。首頁卡片用它顯示徽章。
     */
    val sourceFormat: String = "",
    /**
     * 來源檔案的 Uri（目前只有 PDF 會填）。
     * 匯入時一併取得常駐讀取權限，編輯頁頂欄才有辦法把原始 PDF 重新打開。
     */
    val sourceUri: String = ""
)
