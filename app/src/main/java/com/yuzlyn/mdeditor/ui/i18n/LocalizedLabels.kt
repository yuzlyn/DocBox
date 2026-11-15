package com.yuzlyn.mdeditor.ui.i18n

import com.yuzlyn.mdeditor.data.ImageInsertError
import com.yuzlyn.mdeditor.data.settings.AppLanguage
import com.yuzlyn.mdeditor.data.book.BookError
import com.yuzlyn.mdeditor.data.book.BookException
import com.yuzlyn.mdeditor.data.book.ReadingTheme
import com.yuzlyn.mdeditor.data.importer.ImportError
import com.yuzlyn.mdeditor.data.settings.NoteBackground
import com.yuzlyn.mdeditor.data.settings.NoteBackgroundInk
import com.yuzlyn.mdeditor.data.settings.WritingGuide

/**
 * 資料層的列舉各自帶一個寫死的中文 `label`，那是給日誌與除錯看的。
 *
 * 介面上要顯示名字時一律走這裡，換語言才跟得上；否則設定頁會出現
 * 「英文介面配一張中文色卡」這種半套的狀態。
 */
fun NoteBackground.label(strings: AppStrings): String = when (this) {
    NoteBackground.DEFAULT -> strings.backdropDefault
    NoteBackground.PAPER -> strings.backdropRice
    NoteBackground.SEPIA -> strings.backdropCream
    NoteBackground.MINT -> strings.backdropMint
    NoteBackground.SKY -> strings.backdropSky
    NoteBackground.ROSE -> strings.backdropRose
    NoteBackground.GRAPHITE -> strings.backdropGraphite
    NoteBackground.INK -> strings.backdropInk
}

fun WritingGuide.label(strings: AppStrings): String = when (this) {
    WritingGuide.NONE -> strings.guideNone
    WritingGuide.RULED -> strings.guideRuled
    WritingGuide.GRID -> strings.guideGrid
}

fun NoteBackgroundInk.label(strings: AppStrings): String = when (this) {
    NoteBackgroundInk.AUTO -> strings.noteBackgroundInkAuto
    NoteBackgroundInk.DARK -> strings.noteBackgroundInkDark
    NoteBackgroundInk.LIGHT -> strings.noteBackgroundInkLight
}

/** 閱讀器的五套紙張。資料層只留了中英兩份名字，這裡補齊四語。 */
fun ReadingTheme.label(strings: AppStrings): String = when (this) {
    ReadingTheme.DEFAULT -> strings.readerThemeDefault
    ReadingTheme.PAPER -> strings.readerThemePlain
    ReadingTheme.CREAM -> strings.readerThemeTinted
    ReadingTheme.NIGHT -> strings.readerThemeDim
    ReadingTheme.INK -> strings.readerThemeInk
}

// ── 資料層的錯誤 ────────────────────────────────────────────────────────
//
// 解析書、匯入檔案、插入圖片這三條路徑以前各自把中文句子往下丟，UI 直接顯示
// `exception.message`。切成英文或法文時，那幾句就是整個介面裡唯一的殘留中文。
// 現在資料層只帶 `BookError`／`ImportError`／`ImageInsertError`，句子在這裡組。

/** 技術細節接在句子後面，例如「章節檔案讀不出來（OEBPS/ch3.xhtml）」。 */
private fun String.withDetail(detail: String?): String =
    if (detail.isNullOrBlank()) this else "$this（$detail）"

fun AppStrings.bookError(error: BookError, detail: String? = null): String = when (error) {
    BookError.UNKNOWN_FORMAT -> bookErrUnknownFormat
    BookError.INVALID_PATH -> bookErrInvalidPath
    BookError.FILE_NOT_FOUND -> bookErrFileNotFound
    BookError.FILE_UNREADABLE -> bookErrFileUnreadable
    BookError.FILE_EMPTY -> bookErrFileEmpty
    BookError.FILE_TOO_SMALL -> bookErrFileTooSmall
    BookError.NOT_EPUB -> bookErrNotEpub
    BookError.NOT_MOBI -> bookErrNotMobi
    BookError.NOT_FB2 -> bookErrNotFb2
    BookError.EPUB_NO_ROOTFILE -> bookErrEpubRootfile
    BookError.EPUB_OPF_UNREADABLE -> bookErrEpubPackage
    BookError.NO_CONTENT -> bookErrNoContent
    BookError.CHAPTER_NOT_FOUND -> bookErrChapterMissing
    BookError.CHAPTER_UNREADABLE -> bookErrChapterUnreadable
    BookError.DRM_PROTECTED -> bookErrDrm
    BookError.CORRUPT_STRUCTURE -> bookErrCorrupt
    BookError.UNSUPPORTED_COMPRESSION -> bookErrCompression
    BookError.PARSE_FAILED -> bookErrParse
    BookError.OPEN_FAILED -> bookErrOpen
}.withDetail(detail)

fun AppStrings.importError(error: ImportError, detail: String? = null): String = when (error) {
    ImportError.FILE_UNREADABLE -> importErrUnreadable
    ImportError.PDF_NO_EXTENSION -> importErrPdfNoExtension
    ImportError.WORD_UNREADABLE -> importErrWord
    ImportError.EMPTY -> importErrEmpty
    ImportError.HTML_NO_TEXT -> importErrHtmlEmpty
    ImportError.EBOOK_USE_READER -> importErrEbook
    ImportError.UNSUPPORTED -> importErrUnsupported
    ImportError.PDF_UNREADABLE -> importErrPdfUnreadable
    ImportError.PDF_NO_TEXT_LAYER -> importErrPdfNoText
    ImportError.PDF_PARSE_FAILED -> importErrPdfParse
}.withDetail(detail)

fun AppStrings.imageInsertError(
    error: ImageInsertError,
    detail: String? = null
): String = when (error) {
    ImageInsertError.NOTE_NOT_LOADED -> imageErrNotReady
    ImageInsertError.PERMISSION_DENIED -> imageErrPermission
    ImageInsertError.UNKNOWN -> imageErrUnknown
    ImageInsertError.NOT_WRITABLE -> imageErrNotWritable
    ImageInsertError.UNSUPPORTED_FORMAT -> imageErrFormat
    ImageInsertError.IMAGES_DIR_FAILED -> imageErrDir
    ImageInsertError.CREATE_FAILED -> imageErrCreate
    ImageInsertError.COPY_FAILED -> imageErrCopy
}.withDetail(detail)

/**
 * 把開書丟出來的例外變成一句話。
 *
 * 只認 [BookException]：其他例外（IO、OOM 之類）屬於「說不清的錯」，
 * 由呼叫端給一句籠統的句子，總比把 `java.io.IOException: ...` 攤在讀者面前好。
 */
fun Throwable.bookMessage(strings: AppStrings, fallback: String): String =
    if (this is BookException) strings.bookError(error, detail) else fallback

/**
 * 介面語言對應的 [java.util.Locale]。
 *
 * 有自己的語言設定時不能用 `Locale.getDefault()`：系統是中文、App 切成英文時，
 * 熱力圖的月份會印成「11月」，跟旁邊的英文標籤對不上。
 */
fun AppLanguage.locale(): java.util.Locale = when (this.resolved()) {
    AppLanguage.SYSTEM -> java.util.Locale.getDefault()
    AppLanguage.ZH_TW -> java.util.Locale.TRADITIONAL_CHINESE
    AppLanguage.ZH_CN -> java.util.Locale.SIMPLIFIED_CHINESE
    AppLanguage.EN_US -> java.util.Locale.US
    AppLanguage.FR_FR -> java.util.Locale.FRANCE
}
