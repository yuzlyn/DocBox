package com.yuzlyn.mdeditor.data.settings

import com.yuzlyn.mdeditor.data.book.ReaderMode
import com.yuzlyn.mdeditor.data.book.ReadingTheme
import com.yuzlyn.mdeditor.data.export.CardPosition
import java.util.Locale

enum class AppLanguage(val tag: String, val nativeName: String) {
    ZH_TW("zh-TW", "繁體中文"),
    ZH_CN("zh-CN", "简体中文"),
    EN_US("en-US", "English"),
    FR_FR("fr-FR", "Français");

    companion object {
        fun fromTag(tag: String?): AppLanguage =
            entries.firstOrNull { it.tag.equals(tag, ignoreCase = true) } ?: systemDefault()

        /** 系統語言未適配時一律回落英語，與桌面顯示名稱的規則一致。 */
        fun systemDefault(): AppLanguage {
            val locale = Locale.getDefault()
            return when (locale.language.lowercase(Locale.ROOT)) {
                "zh" -> if (locale.country.equals("CN", ignoreCase = true)) ZH_CN else ZH_TW
                "fr" -> FR_FR
                else -> EN_US
            }
        }
    }
}

/** 取色方式：系統壁紙莫奈取色，或由自訂種子色派生。 */
enum class ThemeMode { MONET, CUSTOM_SEED }

/**
 * 自訂取色的種子色；實際配色由 MaterialKolor 的 TonalSpot 色票派生。
 *
 * 每顆種子都對應桌面圖標的一套配色（见 ui.theme.LauncherIcon）：
 * 切換種子色時，圖標背景同步換成該種子派生出的 primary，App 內主色與桌面圖標才是一套。
 *
 * [ThemeMode.MONET] 走另一條路：主色由壁紙算出、枚舉不完，只能從 ui.theme.LauncherPalette
 * 的色輪裡就近匹配一套 —— 近似，但同樣是「跟著主色走」。
 */
enum class SeedColor(val argb: Long) {
    LAVENDER(0xFF6750A4),
    OCEAN(0xFF00696D),
    FOREST(0xFF2E6B4F),
    CLAY(0xFF8B5000),
    ROSE(0xFF8E4585),
    SLATE(0xFF4F5B92),

    /** 莫奈《睡蓮》淺水區取樣 #5D8895；TonalSpot 派生 primary = #396471。 */
    LOTUS(0xFF5D8895);

    /** 啟動器別名裡的配色標籤：`LOTUS` → `Lotus`，拼出 `LauncherXiaLotus`。 */
    val aliasTag: String get() = name.lowercase().replaceFirstChar { it.titlecase() }
}

enum class DarkModeOption { FOLLOW_SYSTEM, DARK, LIGHT }

/** 筆記列表排版：瀑布流（雙欄）或單列。 */
enum class NoteLayout { MASONRY, LIST }

/**
 * 側欄上的幾個主頁。
 *
 * 這幾個是**平級**的（不是筆記裡再開一層書庫或統計）：抽屜裡隨時互換，
 * 返回鍵在任何一頁都一樣是離開 App。書庫只有一個視圖——所有書同一個格子，
 * 資料夾是「自動收書」的來源，不是另一種瀏覽層級；統計則是一頁看完長期趨勢，
 * 不掛在任何一頁底下。
 */
enum class HomeTarget(val id: String) {
    NOTES("notes"),
    LIBRARY("library"),
    STATS("stats");

    /** 是否落在書庫這一個主頁。 */
    val isLibrary: Boolean get() = this == LIBRARY

    /** 是否落在統計頁。 */
    val isStats: Boolean get() = this == STATS

    companion object {
        /**
         * `folders` 是舊版的第三檔（資料夾總覽），已取消。
         * 當初選過它的人要留在書庫，不是被踢回筆記頁——所以這裡特別接一下。
         */
        fun fromId(id: String?): HomeTarget = when (id) {
            LIBRARY.id, "folders" -> LIBRARY
            STATS.id -> STATS
            else -> NOTES
        }
    }
}

/**
 * 書寫輔助線（App 內稱「書寫輔助線」，對齊 Apple Notes 的 Lines & Grids／日文的罫線・原稿用紙）。
 *
 * - [NONE]：空白紙。
 * - [RULED]：分隔線。橫排是橫線、直排是**豎線**（分的是欄，不是行）。
 * - [GRID]：作文格。兩個方向都是正方形格子（1em × 1em），不是欄距那種長方形。
 */
enum class WritingGuide(val id: String, val label: String) {
    NONE("none", "無"),
    RULED("ruled", "分隔線"),
    GRID("grid", "作文格");

    companion object {
        fun fromId(id: String?): WritingGuide =
            entries.firstOrNull { it.id == id } ?: NONE
    }
}

/** 筆記排序：自訂（預設，保持資料庫順序）、建立日期、修改日期。 */
enum class NoteSort { CUSTOM, CREATED, MODIFIED }

/** 卡片形狀：四種圓角預設（dp）。 */
enum class CardShape(val radiusDp: Int) {
    DEFAULT(16),
    ROUNDED(24),
    EXTRA_ROUNDED(32),
    SQUARE(4)
}

/**
 * 編輯頁背景預設。
 *
 * [DEFAULT] 完全跟隨主題（Monet 取色／深色模式），所以系統切亮暗時會跟著換。
 * 其餘每個都是**固定色**，並自帶配套的文字色、頂欄色、底欄色與強調色；
 * 一旦選定，系統切亮暗都不會把顏色翻掉，字也不會突然看不清。
 *
 * 每個 preset 的 `onSurface` 對 `surface`、`accent` 對 `surface` 都維持 4.5:1 以上對比。
 */
enum class NoteBackground(
    val id: String,
    val label: String,
    val surface: Long? = null,
    val onSurface: Long? = null,
    val onSurfaceVariant: Long? = null,
    val topBar: Long? = null,
    val bottomBar: Long? = null,
    val accent: Long? = null
) {
    /** 跟隨主題：淺色／深色、Monet 取色都自動適配。 */
    DEFAULT("default", "默認"),

    PAPER(
        id = "paper",
        label = "米紙",
        surface = 0xFFFAF6EF,
        onSurface = 0xFF241F19,
        onSurfaceVariant = 0xFF5D554A,
        topBar = 0xFFF2ECE1,
        bottomBar = 0xFFF2ECE1,
        accent = 0xFF7A5320
    ),

    SEPIA(
        id = "sepia",
        label = "米黃",
        surface = 0xFFF1E4CC,
        onSurface = 0xFF33281A,
        onSurfaceVariant = 0xFF6B5A43,
        topBar = 0xFFE8D9BC,
        bottomBar = 0xFFE8D9BC,
        accent = 0xFF8A5A22
    ),

    MINT(
        id = "mint",
        label = "薄荷",
        surface = 0xFFE7F2EA,
        onSurface = 0xFF1B2A21,
        onSurfaceVariant = 0xFF4E6357,
        topBar = 0xFFDBEBDF,
        bottomBar = 0xFFDBEBDF,
        accent = 0xFF2F6B4F
    ),

    SKY(
        id = "sky",
        label = "天青",
        surface = 0xFFE8F0FA,
        onSurface = 0xFF1A2634,
        onSurfaceVariant = 0xFF4B5A6C,
        topBar = 0xFFDAE7F6,
        bottomBar = 0xFFDAE7F6,
        accent = 0xFF2C5C8A
    ),

    ROSE(
        id = "rose",
        label = "玫瑰",
        surface = 0xFFF9EAEE,
        onSurface = 0xFF2D1E23,
        onSurfaceVariant = 0xFF6A5460,
        topBar = 0xFFF2DDE3,
        bottomBar = 0xFFF2DDE3,
        accent = 0xFF8A3A55
    ),

    GRAPHITE(
        id = "graphite",
        label = "石墨",
        surface = 0xFF1E2023,
        onSurface = 0xFFE8E4DF,
        onSurfaceVariant = 0xFFA8A39D,
        topBar = 0xFF26292D,
        bottomBar = 0xFF26292D,
        accent = 0xFFD0B98D
    ),

    INK(
        id = "ink",
        label = "墨黑",
        surface = 0xFF111214,
        onSurface = 0xFFEDEAE5,
        onSurfaceVariant = 0xFFA29E97,
        topBar = 0xFF1A1C1F,
        bottomBar = 0xFF1A1C1F,
        accent = 0xFFDCC38A
    );

    /** true 表示這是固定色系，不會隨系統亮暗翻轉。 */
    val isFixed: Boolean get() = surface != null

    companion object {
        fun fromId(id: String?): NoteBackground =
            entries.firstOrNull { it.id == id } ?: DEFAULT
    }
}

/**
 * 背景圖片上文字要用深色還是淺色。
 *
 * 自訂圖片不像六套固定色有配好的 onSurface：同一張日落照，上半是天空、下半是地面，
 * 深字淺字都可能局部看不見。與其猜 luminance（一張圖的平均亮度對比度毫無保證），
 * 不如直接問使用者，選完就固定——這是唯一能保證「一定看得見」的做法。
 */
enum class NoteBackgroundInk(val id: String, val label: String) {
    /** 跟隨主題：與純色背景一樣用 MaterialTheme 的 onSurface。 */
    AUTO("auto", "跟隨主題"),
    DARK("dark", "深色字"),
    LIGHT("light", "淺色字");

    companion object {
        fun fromId(id: String?): NoteBackgroundInk =
            entries.firstOrNull { it.id == id } ?: AUTO
    }
}

data class AppSettings(
    val language: AppLanguage = AppLanguage.systemDefault(),
    /** 預設改為固定種子：桌面圖標是靜態資源，跟不了壁紙色，兩者要一致只能固定取色。 */
    val themeMode: ThemeMode = ThemeMode.CUSTOM_SEED,
    val seedColor: SeedColor = SeedColor.LOTUS,
    /** 桌面圖標用的字形；見 [LauncherGlyph]。字形與配色各自獨立，組合起來才是一套圖標。 */
    val launcherGlyph: LauncherGlyph = LauncherGlyph.XIA,
    val darkMode: DarkModeOption = DarkModeOption.FOLLOW_SYSTEM,
    val noteLayout: NoteLayout = NoteLayout.MASONRY,
    val noteSort: NoteSort = NoteSort.CUSTOM,
    val cardShape: CardShape = CardShape.DEFAULT,
    /** 編輯區預設字型：`system` 或使用者匯入的字型檔名（見 data.fonts.FontStore）。 */
    val noteFont: String = "system",
    /** 閱讀器預設字型。編輯與看書是兩種場合，可以各用一種。 */
    val readerFont: String = "system",
    /** 編輯頁背景圖片（絕對路徑）；空字串表示不用圖片，走 [noteBackgroundPreset]。 */
    val noteBackground: String = "",
    /** 編輯頁背景預設；固定色不隨系統亮暗翻轉，`default` 才跟隨主題。 */
    val noteBackgroundPreset: String = NoteBackground.DEFAULT.id,
    /** 背景圖片上的文字顏色；只在使用背景圖片時生效（見 [NoteBackgroundInk]）。 */
    val noteBackgroundInk: String = NoteBackgroundInk.AUTO.id,
    /** 直排：編輯區與預覽皆改為由上而下、由右而左的直書。 */
    val verticalLayout: Boolean = false,
    /** 直排編輯區在每欄頂端顯示行號；同一行折到下一欄的續欄不編號。 */
    val showLineNumbers: Boolean = true,
    /** 新建筆記預設的書寫輔助線；單篇筆記可以蓋掉（見 NoteEntity.writingGuide）。 */
    val writingGuide: String = WritingGuide.NONE.id,
    /** 閱讀器的底紙（見 [ReadingTheme]）；只影響看書，不影響筆記編輯頁。 */
    val readerTheme: String = ReadingTheme.DEFAULT.id,
    /** 閱讀器直排；書自己的 `page-progression-direction` 只在沒設過時參考。 */
    val readerVertical: Boolean = false,
    /** 閱讀器字級百分比，100 = 書自己的大小。 */
    val readerTextScale: Int = 100,
    /** 翻頁方式：整頁翻動或連續滾動（見 [ReaderMode]）。 */
    val readerMode: String = ReaderMode.PAGED.id,
    /** 行距倍數，存成「十分之一」的整數（17 = 1.7）。 */
    val readerLineHeightTenths: Int = 17,
    /** 頁邊留白，單位 dp。 */
    val readerMargin: Int = 20,
    /** 啟動頁：全部筆記或書庫（全部圖書）。 */
    val homeTarget: HomeTarget = HomeTarget.NOTES,
    /**
     * 統計總開關。
     *
     * 關掉之後**什麼都不記**（連檔案都不再寫），側欄也不再出現統計頁——
     * 「可以自訂」到極致就是可以把它整個拿掉，這比留一個空頁面誠實。
     */
    val statsEnabled: Boolean = true,
    /** 記閱讀時長與書目。 */
    val statsTrackReading: Boolean = true,
    /** 記文件的開啟與編輯。 */
    val statsTrackDocuments: Boolean = true,
    /** 統計頁的熱力圖區塊（GitHub 那種格子）。 */
    val statsShowHeatmap: Boolean = true,
    /** 統計頁的排行榜區塊。 */
    val statsShowRanking: Boolean = true,
    /** 分享／匯出時是否附上個人名片。 */
    val cardEnabled: Boolean = true,
    /** 分享／匯出時的名片位置（見 data.export.CardPosition）。 */
    val cardPosition: String = CardPosition.AFTER.id,
    /** 名片上的頭像；只有圖片格式看得到。 */
    val cardShowAvatar: Boolean = true,
    val cardShowNickname: Boolean = true,
    val cardShowEmail: Boolean = true,
    val cardShowGithub: Boolean = true
) {
    /** 目前的書寫輔助線（找不到就回無）。 */
    val guide: WritingGuide
        get() = WritingGuide.fromId(writingGuide)

    /** 目前的背景預設（找不到就回默認）。 */
    val background: NoteBackground
        get() = NoteBackground.fromId(noteBackgroundPreset)

    /** 背景圖片上的文字顏色。 */
    val backgroundInk: NoteBackgroundInk
        get() = NoteBackgroundInk.fromId(noteBackgroundInk)

    /** 名片位置（找不到就回「正文之後」，和舊行為一致）。 */
    val cardPlacement: CardPosition
        get() = CardPosition.fromId(cardPosition)

    /** 目前的閱讀底紙。 */
    val readingTheme: ReadingTheme
        get() = ReadingTheme.fromId(readerTheme)

    /** 目前的翻頁方式。 */
    val readingMode: ReaderMode
        get() {
            val mode = when (readerMode) {
                // 舊版只有「滾動」一檔，軸向當時跟著排版方向走；照原樣換算，不必升級存檔。
                "scroll" -> ReaderMode.defaultFor(readerVertical)
                else -> ReaderMode.fromId(readerMode)
            }
            // 直排＋上下滾動是無效組合：直排的內容在**橫向**上展開，
            // 豎著滾沒有任何可滾的餘量，畫面會卡住不動。舊存檔裡可能留著這個值。
            return if (readerVertical && mode == ReaderMode.SCROLL_VERTICAL) {
                ReaderMode.SCROLL_HORIZONTAL
            } else {
                mode
            }
        }

    /** 目前的行距倍數。 */
    val readingLineHeight: Float
        get() = readerLineHeightTenths.coerceIn(12, 24) / 10f
}
